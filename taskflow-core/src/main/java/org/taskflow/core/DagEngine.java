package org.taskflow.core;

import com.alibaba.ttl.threadpool.TtlExecutors;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.taskflow.common.constant.DagConstant;
import org.taskflow.common.util.DagUtil;
import org.taskflow.core.callback.ICallable;
import org.taskflow.core.callback.IDagCallback;
import org.taskflow.core.context.DagContext;
import org.taskflow.core.enums.DagState;
import org.taskflow.core.enums.ResultState;
import org.taskflow.core.enums.WrapperState;
import org.taskflow.core.event.OperatorEventEnum;
import org.taskflow.core.exception.TaskFlowException;
import org.taskflow.core.listener.OperatorListener;
import org.taskflow.core.operator.OperatorResult;
import org.taskflow.core.util.ConvertUtil;
import org.taskflow.core.wrapper.OperatorWrapper;
import org.taskflow.core.wrapper.OperatorWrapperGroup;

import java.util.*;
import java.util.concurrent.*;

/**
 * DAG执行引擎
 * Created by ytyht226 on 2022/3/16.
 */
@Slf4j
@SuppressWarnings("all")
public class DagEngine {
    /**
     * 工作线程池
     */
    private ExecutorService executor;
    /**
     * 主线程阻塞等待所有结束节点执行完成
     */
    private CountDownLatch syncLatch;
    /**
     * wrapper集合
     */
    private Map<String, OperatorWrapper<?, ?>> wrapperMap = new HashMap<>();
    /**
     * wrapperGroup集合
     */
    private Map<String, OperatorWrapperGroup> wrapperGroupMap;
    /**
     * 编排流程的超时时间，单位：毫秒
     */
    private long timeout;
    /**
     * 工作线程的快照
     */
    private ConcurrentHashMap<Thread, OperatorWrapper<?, ?>> runningThreadMap = new ConcurrentHashMap();
    /**
     * DAG编排流程的回调接口，如果提供了回调接口，编排流程的执行是异步的，流程执行完后回调该接口
     */
    private IDagCallback dagCallback;
    /**
     * DAG引擎执行前回调
     */
    private IDagCallback before;
    /**
     * DAG引擎执行后回调
     */
    private IDagCallback after;
    /**
     * 每个OP执行前的回调
     */
    private ICallable beforeOp;
    /**
     * 每个OP执行后的回调
     */
    private ICallable afterOp;
    /**
     * OP异常堆栈，只保存一个
     */
    private Throwable ex;
    /**
     * 执行的状态
     */
    private int state = DagState.INIT;
    /**
     * 执行引擎上下文
     */
    private DagContext dagContext = new DagContext();
    /**
     * DAG节点之间的依赖关系是否已经解析
     */
    private boolean nextDependParsed = false;
    /**
     * 开始节点结合
     */
    private Set<OperatorWrapper<?, ?>> beginWrapperSet = new HashSet<>();
    /**
     * 结束节点集合
     * 引擎执行过程中可以根据节点执行情况动态设置结束节点，需要使用线程安全的集合
     */
    private Set<OperatorWrapper<?, ?>> endWrapperSet = ConcurrentHashMap.newKeySet();

    public DagEngine(ExecutorService executor) {
        this.executor = TtlExecutors.getTtlExecutorService(executor);
    }

    public DagEngine(Object context, ExecutorService executor) {
        this(executor);
        dagContext.putOperatorResult(DagConstant.REQUEST_CONTEXT_ID, new OperatorResult<>(context, ResultState.SUCCESS));
    }

    /**
     * 阻塞主线程，等待流程执行结束，根据依赖关系自动解析出开始节点
     * @param timeout 超时时间，单位：毫秒
     */
    public void runAndWait(long timeout) {
        //解析依赖
        parseNextDepend();
        if (beginWrapperSet == null || beginWrapperSet.size() == 0) {
            return;
        }
        this.timeout = timeout;
        OperatorWrapper[] beginWrappers = ConvertUtil.set2Array(beginWrapperSet);
        this.getDagInitialTask(beginWrappers).run();
    }

    /**
     * 主线程立即返回，流程执行结束后调用回调接口
     * @param timeout 超时时间，单位：毫秒
     * @param dagCallback 回调接口
     */
    public void runWithCallback(long timeout, IDagCallback dagCallback) {
        //解析依赖
        parseNextDepend();
        if (beginWrapperSet == null || beginWrapperSet.size() == 0) {
            return;
        }
        this.timeout = timeout;
        this.dagCallback = dagCallback;
        OperatorWrapper[] beginWrappers = ConvertUtil.set2Array(beginWrapperSet);
        executor.submit(this.getDagInitialTask(beginWrappers));
    }

    /**
     * 指定结束节点，指定wrapper
     */
    public void stopAt(OperatorWrapper<?, ?> ... endWrappers) {
        if (endWrappers == null) {
            return;
        }
        endWrapperSet.clear();
        endWrapperSet.addAll(Sets.newHashSet(endWrappers));
    }
    /**
     * 指定结束节点，指定wrapperId
     */
    public void stopAt(String ... endWrapperIds) {
        if (endWrapperIds == null) {
            return;
        }
        OperatorWrapper[] endWrappers = new OperatorWrapper[endWrapperIds.length];
        int index = 0;
        for (String endWrapperId : endWrapperIds) {
            OperatorWrapper<?, ?> wrapper = wrapperMap.get(endWrapperId);
            if (wrapper == null) {
                throw new TaskFlowException("id does not exist");
            }
            endWrappers[index++] = wrapper;
        }
        stopAt(endWrappers);
    }

    public DagEngine before(IDagCallback callback) {
        this.before = callback;
        return this;
    }

    public DagEngine after(IDagCallback callback) {
        this.after = callback;
        return this;
    }

    public DagEngine beforeOp(ICallable callback) {
        this.beforeOp = callback;
        return this;
    }

    public DagEngine afterOp(ICallable callback) {
        this.afterOp = callback;
        return this;
    }

    /**
     * 获取初始化线程，主要逻辑如下：
     * 1、执行引擎回调接口
     * 2、初始化引擎上下文
     * 3、初始化阻塞线程监听的信号量
     * 4、异步提交开始节点
     * 5、阻塞主线程
     */
    private Runnable getDagInitialTask(OperatorWrapper<?, ?>... beginWrappers) {
        return () -> {
            try {
                if (before != null) {
                    before.callback();
                }
                state = DagState.RUNNING;
                //设置DAG引擎上下文，上下文的生命周期从开始节点到结束节点之间
                DagContextHolder.set(dagContext);
                //初始化信号量
                syncLatch = new CountDownLatch(endWrapperSet.size());
                //将初始节点交给线程池执行，此过程是异步的
                for (OperatorWrapper<?, ?> wrapper : beginWrappers) {
                    doRun(wrapper, true);
                }
                //线程阻塞等待DAG执行结束，或超时被唤醒
                awaitAndInterruptRunningThread();
                //如果是非阻塞模式，dag执行结束后，调用回调接口
                if (dagCallback != null) {
                    dagCallback.callback();
                }
            } catch (Throwable e) {
                log.error("getDagInitialTask error", e);
            } finally {
                //清理DAG引擎上下文
                DagContextHolder.remove();
                if (after != null) {
                    after.callback();
                }
            }
        };
    }

    /**
     * 主线程阻塞，唤醒后会打断还在执行中的节点
     */
    private void awaitAndInterruptRunningThread() {
        boolean isTimeout = false;
        try {
            boolean awaitResult = syncLatch.await(timeout, TimeUnit.MILLISECONDS);
            if (!awaitResult) {
                state = DagState.ERROR;
                isTimeout = true;
            }
        } catch (InterruptedException e) {
            state = DagState.ERROR;
            log.error("dagEngine is interrupted", e);
        }
        //理论上不会出现这种情况，防御性容错
        if (state == DagState.RUNNING) {
            state = DagState.FINISH;
        }
        if (!runningThreadMap.isEmpty()) {
            for (Map.Entry<Thread, OperatorWrapper<?, ?>> entry : runningThreadMap.entrySet()) {
                Thread thread = entry.getKey();
                thread.interrupt();
                // 封装超时异常
                if (isTimeout && this.ex == null) {
                    state = DagState.ERROR;
                    OperatorWrapper<?, ?> operatorWrapper = entry.getValue();
                    String message = operatorWrapper.getOperator().getClass().getName();
                    this.ex = new TimeoutException(message);
                }
            }
        }
    }

    /**
     * 解析节点之间的依赖关系、开始节点集合、结束节点集合
     */
    private void parseNextDepend() {
        if (nextDependParsed) {
            return;
        }
        nextDependParsed = true;
        //解析节点依赖关系
        for (Map.Entry<String, OperatorWrapper<?, ?>> entry : wrapperMap.entrySet()) {
            OperatorWrapper<?, ?> wrapper = entry.getValue();
            if (!wrapper.isInit()) {
                wrapper.init();
            }
            //根据 next 解析依赖关系
            Map<String, Boolean> nextWrapperIdMap = wrapper.getNextWrapperIdMap();
            if (nextWrapperIdMap != null && nextWrapperIdMap.size() != 0) {
                for (String nextId : nextWrapperIdMap.keySet()) {
                    OperatorWrapper<?, ?> next = wrapperMap.get(nextId);
                    if (next.getDependWrappers() != null && next.getDependWrappers().contains(wrapper)) {
                        continue;
                    }
                    //将当前节点添加到后继节点的依赖集合中
                    if (next.getDependWrappers() == null) {
                        next.setDependWrappers(new HashSet<>());
                    }
                    next.getDependWrappers().add(wrapper);
                    //将后继节点添加到当前节点的后继集合中
                    if (wrapper.getNextWrappers() == null) {
                        wrapper.setNextWrappers(new HashSet<>());
                    }
                    wrapper.getNextWrappers().add(next);
                    //将强依赖该节点的 indegree+1，弱依赖不需要
                    if (nextWrapperIdMap.get(nextId)) {
                        next.getIndegree().incrementAndGet();
                        if (wrapper.getSelfIsMustSet() == null) {
                            wrapper.setSelfIsMustSet(new HashSet<>());
                        }
                        //将后继节点添加到当前节点的强依赖集合
                        wrapper.getSelfIsMustSet().add(next);
                    }
                }
            }
            //根据 depend 解析依赖关系
            Map<String, Boolean> dependWrapperIdMap = wrapper.getDependWrapperIdMap();
            if (dependWrapperIdMap != null && dependWrapperIdMap.size() != 0) {
                for (String dependId : dependWrapperIdMap.keySet()) {
                    OperatorWrapper<?, ?> depend = wrapperMap.get(dependId);
                    if (wrapper.getDependWrappers() != null && wrapper.getDependWrappers().contains(depend)) {
                        continue;
                    }
                    //将前驱节点添加到当前节点的依赖集合中
                    if (wrapper.getDependWrappers() == null) {
                        wrapper.setDependWrappers(new HashSet<>());
                    }
                    wrapper.getDependWrappers().add(depend);
                    //将当前节点添加到前驱节点的后继集合中
                    if (depend.getNextWrappers() == null) {
                        depend.setNextWrappers(new HashSet<>());
                    }
                    depend.getNextWrappers().add(wrapper);
                    //强依赖前驱节点时，indegree+1，弱依赖不需要
                    if (dependWrapperIdMap.get(dependId)) {
                        wrapper.getIndegree().incrementAndGet();
                        if (depend.getSelfIsMustSet() == null) {
                            depend.setSelfIsMustSet(new HashSet<>());
                        }
                        //将当前节点添加到前驱节点的强依赖集合
                        depend.getSelfIsMustSet().add(wrapper);
                    }
                }
            }
        }
        //解析开始节点、结束节点
        for (Map.Entry<String, OperatorWrapper<?, ?>> entry : wrapperMap.entrySet()) {
            OperatorWrapper<?, ?> wrapper = entry.getValue();
            if (wrapper.getDependWrappers() == null || wrapper.getDependWrappers().size() == 0) {
                beginWrapperSet.add(wrapper);
            }
            if (wrapper.getNextWrappers() == null || wrapper.getNextWrappers().size() == 0) {
                endWrapperSet.add(wrapper);
            }
        }
    }

    /**
     * 将节点包装成线程提交给线程池执行，或直接复用当前线程执行
     */
    private void doRun(OperatorWrapper<?, ?> wrapper, boolean inNewThread) {
        //DAG引擎状态不是RUNNING，说明编排已经执行完
        if (state != DagState.RUNNING) {
            return;
        }
        //条件判断，如果满足相应的条件，执行该节点
        if (wrapper.getCondition() != null && wrapper.getWrapperState().get() == WrapperState.INIT) {
            synchronized (wrapper.getCondition()) {
                //当前节点可以执行时，中断弱依赖的节点
                if (wrapper.getCondition().judge(wrapper)) {
                    this.interruptOrUpdateDependSkipState(wrapper);
                } else {
                    //不满足条件判断，且还有执行中的前驱节点时，不执行该节点
                    Set<OperatorWrapper<?, ?>> dependWrappers = wrapper.getDependWrappers();
                    if (dependWrappers != null && dependWrappers.stream().anyMatch(d -> d.getWrapperState().get() <= WrapperState.RUNNING)) {
                        return;
                    }
                }
            }
        }
        //wrapper节点状态不等于INIT，说明该节点已经执行过
        if (!wrapper.compareAndSetState(WrapperState.INIT, WrapperState.RUNNING)) {
            return;
        }
        if (inNewThread) {
            executor.submit(this.getRunningTask(wrapper));
        } else {
            this.getRunningTask(wrapper).run();
        }
    }

    /**
     * 获取执行节点逻辑的线程，主要处理流程逻辑如下：
     * 1、将执行线程绑定到当前节点，添加到工作线程集合，打断时使用
     * 2、执行全局的节点回调接口
     * 3、执行节点主逻辑
     * 4、节点执行异常后，停止引擎
     * 5、节点正常执行完，根据条件选择后续要执行的分支、节点(组)等
     * 6、将节点执行结果保存到上下文、在工作线程集合中删除当前线程、判断是否将信号量减一
     * 7、执行后继节点
     */
    private Runnable getRunningTask(OperatorWrapper wrapper) {
        return () -> {
            Thread thread = Thread.currentThread();
            try {
                wrapper.setThread(thread);
                runningThreadMap.put(thread, wrapper);
                if (beforeOp != null) {
                    beforeOp.call(wrapper);
                }
                if (wrapper.getBefore() != null) {
                    wrapper.getBefore().call(wrapper);
                }
                this.doExecute(wrapper);

                wrapper.compareAndSetState(WrapperState.RUNNING, WrapperState.FINISH);
                wrapper.getOperatorResult().setResultState(ResultState.SUCCESS);
            } catch (Throwable throwable) {
                wrapper.compareAndSetState(WrapperState.RUNNING, WrapperState.ERROR);
                wrapper.getOperatorResult().setResultState(ResultState.EXCEPTION);
                wrapper.getOperatorResult().setEx(throwable);
                this.ex = throwable;

                //当前节点被其它节点强依赖，出现异常中断整个执行流程
                if (wrapper.getSelfIsMustSet() != null && wrapper.getSelfIsMustSet().size() >= 1) {
                    state = DagState.ERROR;
                }
            } finally {
                if (afterOp != null) {
                    afterOp.call(wrapper);
                }
                if (wrapper.getAfter() != null) {
                    wrapper.getAfter().call(wrapper);
                }
                //选择要执行的分支
                chooseBranch(wrapper);
                //选择要执行的后继节点(组)
                chooseOpOrGroup(wrapper);
                //将OP的执行结果保存到上下文
                DagContextHolder.putOperatorResult(wrapper.getId(), wrapper.getOperatorResult());
                //OP执行后的回调
                if (wrapper.getOperatorResult().getEx() == null) {
                    wrapper.getOperator().onSuccess(wrapper.getParam(), wrapper.getOperatorResult());
                } else {
                    wrapper.getOperator().onError(wrapper.getParam(), wrapper.getOperatorResult());
                }

                //节点执行完，释放线程
                wrapper.setThread(null);
                //从工作线程快照中移除该节点的线程
                runningThreadMap.remove(thread);
                //如果是结束节点，则将信号量减一
                boolean isEndOp = false;
                if (endWrapperSet.contains(wrapper)) {
                    isEndOp = true;
                    // 初始状态 endWrapperSet.size = syncLatch
                    // 引擎执行过程中可能出现不相等的情况，比如：
                    // 1、初始状态有3个结束节点(syncLatch=3)，选择完一个分支后结束节点只有一个，
                    // 此时 endWrapperSet < syncLatch，当前节点执行完可以将信号量减一
                    // 2、初始状态有1个结束节点(syncLatch=1)，执行过程中将结束节点设置成流程中的其它节点，
                    // 比如调用了stopAt("5", "6", "7")，此时 endWrapperSet > syncLatch，当前节点
                    // 执行完时不能将信号量减一，否则可能会出现结束节点没有全部执行完时提前唤醒阻塞线程
                    if (endWrapperSet.size() <= syncLatch.getCount()) {
                        syncLatch.countDown();
                    }
                    endWrapperSet.remove(wrapper);
                    //结束节点全部执行完，停止DAG引擎
                    if (endWrapperSet.isEmpty()) {
                        //状态不是 RUNNING，有以下几种情况
                        //1、多线程情况下，同一时刻多个结束节点都执行完，状态被其它线程修改
                        //2、当前节点执行异常，已经设置成ERROR状态
                        //3、引擎执行超时，主线程被唤醒，设置成了ERROR状态
                        state = state == DagState.RUNNING ? DagState.FINISH : state;
                    }
                }
                if (state != DagState.RUNNING) {
                    //停止引擎，唤醒阻塞线程
                    shutdown(state);
                }
                //通知后继节点
                if (!isEndOp) {
                    notifyNextWrappers(wrapper, wrapper.getNextWrappers());
                }
            }
        };
    }

    /**
     * 根据当前节点的结果选择要执行的分支（分支不能有交叉，类似二叉树）
     * 主要实现逻辑：
     * 1、将未选择的节点从当前节点的后继节点集合中删除
     * 2、将未选择的分支的叶子节点从结束节点集合中删除
     */
    private void chooseBranch(OperatorWrapper wrapper) {
        if (state != DagState.RUNNING || wrapper.getChooseBranch() == null) {
            return;
        }
        try {
            Set<OperatorWrapper<?, ?>> nextWrappers = wrapper.getNextWrappers();
            if (nextWrappers == null || nextWrappers.size() == 1) {
                return;
            }
            //要执行的后继节点
            Set<String> chooseIdSet = wrapper.getChooseBranch().choose(wrapper);
            if (chooseIdSet.size() == nextWrappers.size()) {
                return;
            }

            //当前节点待删除的后继节点集合
            Set<OperatorWrapper<?, ?>> removeNextSet = new HashSet<>();
            for (OperatorWrapper<?, ?> next : nextWrappers) {
                if (chooseIdSet.contains(next.getId())) {
                    continue;
                }
                removeNextSet.add(next);
            }
            nextWrappers.removeAll(removeNextSet);
            Set<OperatorWrapper<?, ?>> nonChooseBranchEndOps = findBranchEndOps(removeNextSet);
            endWrapperSet.removeAll(nonChooseBranchEndOps);
        } catch (Throwable e) {
            log.error("chooseBranch error", e);
        }
    }

    /**
     * 查找分支的叶子节点
     */
    private Set<OperatorWrapper<?, ?>> findBranchEndOps(Set<OperatorWrapper<?, ?>> ops) {
        Set<OperatorWrapper<?, ?>> endOps = new HashSet<>(4);
        if (ops == null || ops.size() == 0) {
            return endOps;
        }
        for (OperatorWrapper<?, ?> curr : ops) {
            findEndOp(endOps, curr);
        }
        return endOps;
    }
    private void findEndOp(Set<OperatorWrapper<?, ?>> endOps, OperatorWrapper<?, ?> curr) {
        if (curr == null) {
            return;
        }
        if (curr.getNextWrappers() == null) {
            endOps.add(curr);
            return;
        }
        for (OperatorWrapper next : curr.getNextWrappers()) {
            findEndOp(endOps, next);
        }
    }

    /**
     * 根据当前节点的结果选择要执行的后继节点(组)
     * 待选择的节点必须具有相同的后继节点(组)，或者待选择的节点本身就是叶子节点
     * 主要实现逻辑：
     * 1、将未选择的节点从当前节点的后继节点集合中删除
     * 2、将未选择的节点从选择节点的后继节点的依赖节点集合中删除
     * 需要考虑一些特殊情况：待选择节点是叶子节点、节点组等情况
     */
    private void chooseOpOrGroup(OperatorWrapper wrapper) {
        if (state != DagState.RUNNING || wrapper.getChooseOp() == null) {
            return;
        }
        try {
            Set<OperatorWrapper<?, ?>> nextWrappers = wrapper.getNextWrappers();
            //后继节点只有一个时直接执行
            if (nextWrappers == null || nextWrappers.size() == 1) {
                return;
            }
            //要执行的后继节点
            Set<String> chooseIdSet = wrapper.getChooseOp().choose(wrapper);
            if (chooseIdSet.size() == nextWrappers.size()) {
                return;
            }
            //要执行的后继节点是否为结束节点（都是或都不是）
            boolean hasEndOp = false;
            // 当前节点待删除的后继节点集合，包含3种节点类型：
            // 1、普通节点
            // 2、节点组的开始节点，前继节点删除后继节点时使用
            // 3、节点组的结束节点，后继节点删除前继节点时使用
            Set<OperatorWrapper<?, ?>> removeNextSet = new HashSet<>(4);
            //要执行的节点是结束节点的集合
            Set<OperatorWrapper<?, ?>> chooseNextEndSet = null;
            //要执行的节点的后继节点(组)
            Set<OperatorWrapper<?, ?>> nextNextSet = new HashSet<>(4);

            for (OperatorWrapper<?, ?> next : nextWrappers) {
                if (chooseIdSet.contains(next.getId())) {
                    //要执行的节点，有两种情况：
                    //1、普通节点
                    //2、节点组的开始节点，需要根据节点组的结束节点找到节点组的后继节点
                    OperatorWrapper<?, ?> realNext = null;
                    if (DagUtil.isGroupBeginOp(next.getId())) {
                        realNext = wrapperMap.get(next.getGroup().getGroupEndId());
                    } else {
                        realNext = next;
                    }
                    nextNextSet.addAll(realNext.getNextWrappers());
                    if (endWrapperSet.contains(realNext)) {
                        hasEndOp = true;

                        if (chooseNextEndSet == null) {
                            chooseNextEndSet = new HashSet<>(4);
                        }
                        chooseNextEndSet.add(realNext);
                    }
                    continue;
                }

                removeNextSet.add(next);
                if (DagUtil.isGroupBeginOp(next.getId())) {
                    OperatorWrapperGroup wrapperGroup = next.getGroup();
                    //将节点组的结束节点加入到待删除列表
                    removeNextSet.add(wrapperMap.get(wrapperGroup.getGroupEndId()));
                }
            }
            //当前节点删除未选择的后继节点集合
            nextWrappers.removeAll(removeNextSet);
            //如果要执行的节点是结束节点，则其它未选择的必须也是结束节点，将选择的节点设置为新的结束节点
            if (hasEndOp) {
                endWrapperSet.clear();
                endWrapperSet.addAll(chooseNextEndSet);
            } else {
                //将选择节点的后继节点的依赖集合删除未选择的节点，如果是强依赖将入度减一
                for (OperatorWrapper<?, ?> nextNext : nextNextSet) {
                    for (OperatorWrapper<?, ?> depend : removeNextSet) {
                        nextNext.getDependWrappers().remove(depend);
                        if (depend.getSelfIsMustSet() != null && depend.getSelfIsMustSet().contains(nextNext)) {
                            nextNext.getIndegree().decrementAndGet();
                        }
                    }
                }
            }
        } catch (Throwable e) {
            log.error("chooseOpOrGroup error", e);
        }
    }

    /**
     * 停止DAG引擎，唤醒阻塞线程
     */
    private void shutdown(int currState){
        state = currState;
        while (syncLatch.getCount() >= 1) {
            syncLatch.countDown();
        }
    }

    /**
     * 执行当前节点的后继节点，主要逻辑如下：
     * 1、后继节点强依赖当前节点，将入度减一（弱依赖时，不计入入度）
     * 2、如果后继节点入度不为零，不执行
     * 3、如果后继节点有弱依赖的节点时，后继节点在执行时可以将还在执行中或未执行的依赖节点中断
     */
    private void notifyNextWrappers(OperatorWrapper<?, ?> wrapper, Set<OperatorWrapper<?, ?>> nextWrappers) {
        if (nextWrappers == null || state != DagState.RUNNING) {
            return;
        }
        Set<OperatorWrapper<?, ?>> selfIsMustSet = wrapper.getSelfIsMustSet();
        List<OperatorWrapper<?, ?>> needRunWrappers = null;
        for (OperatorWrapper<?, ?> next : nextWrappers) {
            //当前节点被后继节点强依赖时，需要将后继节点的 indegree-1
            if (selfIsMustSet != null && selfIsMustSet.contains(next)) {
                next.getIndegree().decrementAndGet();
            }
            //强依赖的节点没有执行完时，节点不能开始执行
            if (next.getIndegree().get() != 0) {
                continue;
            }
            //节点不能重复执行
            if (next.getWrapperState().get() != WrapperState.INIT) {
                continue;
            }
            if (needRunWrappers == null) {
                needRunWrappers = new ArrayList<>();
            }
            needRunWrappers.add(next);
        }
        if (needRunWrappers != null) {
            for (int i = 0; i < needRunWrappers.size(); i++) {
                OperatorWrapper<?, ?> nextWrapper = needRunWrappers.get(i);
                boolean inNewThread = true;
                //执行最后一个后继节点时复用当前线程，其它的节点交给线程池执行
                if (i == needRunWrappers.size() - 1) {
                    inNewThread = false;
                }
                doRun(nextWrapper, inNewThread);
                //执行后继节点前，中断正在执行的、跳过还未开始执行的弱依赖的前驱节点，
                //如果存在条件判断时，在doRun方法中根据是否满足条件进行中断或跳过
                if (nextWrapper.getCondition() == null) {
                    this.interruptOrUpdateDependSkipState(nextWrapper);
                }
            }
        }
    }

    /**
     * 中断当前节点弱依赖的前继节点，主要逻辑如下：
     * 1、如果前继节点有多个后继节点 或者 有被强依赖的后继节点，则不能被中断，否则可能会影响其它节点的执行
     * 2、打断执行中的前继节点
     * 3、前继节点还未开始执行，将状态设置为跳过，并递归中断前继节点的前继节点
     */
    private void interruptOrUpdateDependSkipState(OperatorWrapper<?, ?> wrapper) {
        Set<OperatorWrapper<?, ?>> dependWrappers = wrapper.getDependWrappers();
        for (OperatorWrapper<?, ?> depend : dependWrappers) {
            //强依赖的节点不能中断或跳过
            if (depend.getNextWrappers().size() > 1 || depend.getSelfIsMustSet() != null) {
                continue;
            }
            //中断执行中的弱依赖节点的线程
            if (depend.getWrapperState().get() == WrapperState.RUNNING) {
                depend.getThread().interrupt();
            } else if (depend.getWrapperState().get() == WrapperState.INIT) {
                //将还未开始执行的节点状态修改成 skip
                depend.compareAndSetState(WrapperState.INIT, WrapperState.SKIP);
                this.interruptOrUpdateDependSkipState(depend);
            }
        }
    }

    /**
     * 调用目标operator的 execute 方法，主要逻辑如下：
     * 1、执行节点监听器（开始、异常、成功）
     * 2、执行节点回调接口
     * 3、执行节点主逻辑
     */
    private void doExecute(OperatorWrapper wrapper) {
        Object param = parseOperatorParam(wrapper);
        wrapper.setParam(param);
        Object operatorResult = null;
        try {
            //OP监听器--开始前
            if (wrapper.getListener(OperatorEventEnum.START) != null) {
                wrapper.getListener(OperatorEventEnum.START).forEach(t -> ((OperatorListener)t).onEvent(wrapper, OperatorEventEnum.START));
            }
            //OP执行前回调
            wrapper.getOperator().onStart(param);
            //执行目标OP方法
            operatorResult = wrapper.getOperator().execute(param);
        } catch (Exception e) {
            //节点执行异常时，设置operator的默认值
            wrapper.getOperatorResult().setResult(wrapper.getOperator().defaultValue());

            //OP监听器--异常
            if (wrapper.getListener(OperatorEventEnum.ERROR) != null) {
                wrapper.getListener(OperatorEventEnum.ERROR).forEach(t -> ((OperatorListener)t).onEvent(wrapper, OperatorEventEnum.ERROR));
            }
            throw new TaskFlowException(e);
        }
        //OP监听器--成功
        if (wrapper.getListener(OperatorEventEnum.SUCCESS) != null) {
            wrapper.getListener(OperatorEventEnum.SUCCESS).forEach(t -> ((OperatorListener)t).onEvent(wrapper, OperatorEventEnum.SUCCESS));
        }
        wrapper.getOperatorResult().setResult(operatorResult);
    }

    /**
     * 解析 Op 参数列表
     */
    private Object parseOperatorParam(OperatorWrapper wrapper) {
        Object param = null;
        //解析OP执行时需要透传的参数
        {
            List list = null;
            //参数来源是外部变量(请求上下文)
            if (null != wrapper.getContext()) {
                list = new ArrayList();
                list.add(wrapper.getContext());
            }
            //参数来源是其它OP结果
            if (null != wrapper.getParamFromList()) {
                if (list == null) {
                    list = new ArrayList();
                }
                for (Object fromWrapperId : wrapper.getParamFromList()) {
                    list.add(this.wrapperMap.get(fromWrapperId).getOperatorResult().getResult());
                }
            }
            //只有一个变量时
            if (list != null && list.size() == 1) {
                param = list.get(0);
            } else {
                param = list;
            }
        }
        //没有指定OP参数来源时，默认使用请求上下文（如果有）
        if (param == null) {
            if (dagContext.getOperatorResult(DagConstant.REQUEST_CONTEXT_ID) != null) {
                param = dagContext.getOperatorResult(DagConstant.REQUEST_CONTEXT_ID).getResult();
            }
        }
        return param;
    }

    public ExecutorService getExecutor() {
        return this.executor;
    }

    public Map<String, OperatorWrapper<?, ?>> getWrapperMap() {
        return wrapperMap;
    }

    public ConcurrentHashMap<Thread, OperatorWrapper<?, ?>> getRunningThreadMap() {
        return runningThreadMap;
    }

    public Throwable getEx() {
        return ex;
    }

    public DagContext getDagContext() {
        return dagContext;
    }

    public Map<String, OperatorWrapperGroup> getWrapperGroupMap() {
        return wrapperGroupMap;
    }

    public void setWrapperGroupMap(Map<String, OperatorWrapperGroup> wrapperGroupMap) {
        this.wrapperGroupMap = wrapperGroupMap;
    }

    public Set<OperatorWrapper<?, ?>> getBeginWrapperSet() {
        return beginWrapperSet;
    }

    public Set<OperatorWrapper<?, ?>> getEndWrapperSet() {
        return endWrapperSet;
    }

    public CountDownLatch getSyncLatch() {
        return syncLatch;
    }
}
