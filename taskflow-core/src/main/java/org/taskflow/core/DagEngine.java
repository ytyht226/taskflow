package org.taskflow.core;

import com.alibaba.ttl.threadpool.TtlExecutors;
import lombok.extern.slf4j.Slf4j;
import org.taskflow.core.callback.ICallable;
import org.taskflow.core.callback.IDagCallback;
import org.taskflow.core.context.DagContext;
import org.taskflow.core.enums.DagState;
import org.taskflow.core.enums.ResultState;
import org.taskflow.core.enums.WrapperState;
import org.taskflow.core.event.OperatorEventEnum;
import org.taskflow.core.exception.TaskFlowException;
import org.taskflow.core.listener.OperatorListener;
import org.taskflow.core.wrapper.OperatorWrapper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
     * 主线程阻塞等待所有节点执行结束
     */
    private CountDownLatch syncLatch;
    /**
     * wrapper集合
     */
    private Map<String, OperatorWrapper<?, ?>> wrapperMap = new HashMap<>();
    /**
     * 编排流程的超时时间
     */
    private long timeout;
    /**
     * 工作线程的快照
     */
    private ConcurrentHashMap.KeySetView<Thread, Boolean> runningThreadSet = ConcurrentHashMap.newKeySet();
    /**
     * 手动指定的结束节点
     */
    private ConcurrentHashMap.KeySetView<OperatorWrapper<?, ?>, Boolean> endpointSet;
    /**
     * 手动指定的结束节点全部执行完毕时，由获取到锁的线程将还没有执行完的工作线程中断
     */
    private Lock endpointLock;
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

    public DagEngine(ExecutorService executor) {
        this.executor = TtlExecutors.getTtlExecutorService(executor);
    }

    /**
     * 阻塞主线程，等待流程执行结束，根据依赖关系自动解析出开始节点
     */
    public void runAndWait(long timeout) {
        OperatorWrapper<?, ?>[] beginWrappers = this.parseNextDependAndGetBeginWrappers();
        this.runAndWait(timeout, beginWrappers);
    }

    /**
     * 阻塞主线程，等待流程执行结束，手动指定开始节点
     */
    public void runAndWait(long timeout, OperatorWrapper<?, ?>... beginWrappers) {
        if (beginWrappers == null || beginWrappers.length == 0) {
            return;
        }
        this.timeout = timeout;
        this.getDagInitialTask(beginWrappers).run();
    }

    /**
     * 主线程立即返回，流程执行结束后调用回调接口
     */
    public void runWithCallback(long timeout, IDagCallback dagCallback) {
        OperatorWrapper<?, ?>[] beginWrappers = this.parseNextDependAndGetBeginWrappers();
        this.runWithCallback(timeout, dagCallback, beginWrappers);
    }

    /**
     * 主线程立即返回，流程执行结束后调用回调接口，手动指定开始节点
     */
    public void runWithCallback(long timeout, IDagCallback dagCallback, OperatorWrapper<?, ?>... beginWrappers) {
        if (beginWrappers == null) {
            return;
        }
        this.timeout = timeout;
        this.dagCallback = dagCallback;
        executor.submit(this.getDagInitialTask(beginWrappers));
    }

    /**
     * 指定结束节点
     */
    public static void stopAt(OperatorWrapper<?, ?>... endWrappers) {
        if (endWrappers == null || endWrappers.length == 0) {
            return;
        }
        for (OperatorWrapper endWrapper : endWrappers) {
            endWrapper.getEngine().endpoint(endWrapper);
        }
    }

    /**
     * 指定结束节点
     */
    private DagEngine endpoint(OperatorWrapper<?, ?>... endWrappers) {
        if (endWrappers == null || endWrappers.length == 0) {
            return this;
        }
        synchronized (this) {
            if (endpointSet == null) {
                endpointSet = ConcurrentHashMap.newKeySet();
                endpointLock = new ReentrantLock();
            }
            endpointSet.addAll(Arrays.asList(endWrappers));
        }
        return this;
    }

    /**
     * 指定结束节点
     */
    private DagEngine endpoint(String... endWrapperIds) {
        if (endWrapperIds == null || endWrapperIds.length == 0) {
            return this;
        }
        Set<OperatorWrapper<?, ?>> endWrapperSet = new HashSet<>();
        for (String wrapperId : endWrapperIds) {
            OperatorWrapper<?, ?> beginWrapper = wrapperMap.get(wrapperId);
            if (beginWrapper == null) {
                throw new TaskFlowException("id does not exist");
            }
            endWrapperSet.add(beginWrapper);
        }
        OperatorWrapper<?, ?>[] endWrappers = new OperatorWrapper[endWrapperSet.size()];
        return this.endpoint(endWrapperSet.toArray(endWrappers));
    }

    /**
     * 解析依赖并返回开始节点
     */
    private OperatorWrapper<?, ?>[] parseNextDependAndGetBeginWrappers() {
        parseNextDepend();
        Set<OperatorWrapper<?, ?>> beginWrapperSet = new HashSet<>();
        for (Map.Entry<String, OperatorWrapper<?, ?>> entry : wrapperMap.entrySet()) {
            OperatorWrapper<?, ?> wrapper = entry.getValue();
            if (wrapper.getDependWrappers() == null || wrapper.getDependWrappers().size() == 0) {
                beginWrapperSet.add(wrapper);
            }
        }
        OperatorWrapper<?, ?>[] beginWrappers = new OperatorWrapper[beginWrapperSet.size()];
        return beginWrapperSet.toArray(beginWrappers);
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

    private Runnable getDagInitialTask(OperatorWrapper<?, ?>... beginWrappers) {
        return () -> {
            try {
                //解析依赖
                parseNextDepend();
                if (before != null) {
                    before.callback();
                }
                state = DagState.RUNNING;
                //设置DAG引擎上下文，上下文的生命周期从开始节点到结束节点之间
                DagContextHolder.set(dagContext);
                syncLatch = new CountDownLatch(wrapperMap.size());
                //将初始节点放到线程池执行，此过程是异步的
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

    private void awaitAndInterruptRunningThread() {
        try {
            syncLatch.await(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("dagEngine is interrupted", e);
        }
        state = DagState.FINISH;
        if (!runningThreadSet.isEmpty()) {
            for (Thread thread : runningThreadSet) {
                thread.interrupt();
            }
        }
    }

    private void parseNextDepend() {
        if (nextDependParsed) {
            return;
        }
        nextDependParsed = true;
        for (Map.Entry<String, OperatorWrapper<?, ?>> entry : wrapperMap.entrySet()) {
            OperatorWrapper<?, ?> wrapper = entry.getValue();
            if (!wrapper.isInit()) {
                wrapper.init();
            }

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
    }

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

    private Runnable getRunningTask(OperatorWrapper wrapper) {
        return () -> {
            try {
                wrapper.setThread(Thread.currentThread());
                runningThreadSet.add(Thread.currentThread());

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
                    this.endpoint(wrapper);
                    endpointSet.clear();
                    checkEndpointFinish();
                }
            } finally {
                if (afterOp != null) {
                    afterOp.call(wrapper);
                }
                if (wrapper.getAfter() != null) {
                    wrapper.getAfter().call(wrapper);
                }
                //选择要执行的后继节点
                chooseOp(wrapper);
                //将OP的执行结果保存到上下文
                DagContextHolder.putOperatorResult(wrapper.getId(), wrapper.getOperatorResult());
                //OP执行后的回调
                if (wrapper.getOperatorResult().getEx() != null) {
                    wrapper.getOperator().onSuccess(wrapper.getParam(), wrapper.getOperatorResult());
                } else {
                    wrapper.getOperator().onError(wrapper.getParam(), wrapper.getOperatorResult());
                }

                //节点执行完，释放线程
                wrapper.setThread(null);
                //从工作线程快照中移除该节点的线程
                runningThreadSet.remove(Thread.currentThread());
                //如果当前节点是结束节点，需要判断是否需要唤醒主线程
                if (endpointSet != null) {
                    endpointSet.remove(wrapper);
                    checkEndpointFinish();
                }
                syncLatch.countDown();
                //通知后继节点
                notifyNextWrappers(wrapper, wrapper.getNextWrappers());
            }
        };
    }

    /**
     * 根据当前节点的结果选择要执行的后继节点
     */
    private void chooseOp(OperatorWrapper wrapper) {
        try {
            if (wrapper.getChoose() == null) {
                return;
            }
            Set<OperatorWrapper<?, ?>> nextWrappers = wrapper.getNextWrappers();
            if (nextWrappers == null || nextWrappers.size() == 1) {
                return;
            }
            //要执行的后继节点
            Set<String> chooseIdSet = wrapper.getChoose().choose(wrapper);
            //当前节点待删除的后继节点集合
            Set<OperatorWrapper<?, ?>> removeNextSet = new HashSet<>();
            for (OperatorWrapper<?, ?> next : nextWrappers) {
                if (chooseIdSet.contains(next.getId())) {
                    continue;
                }
                removeNextSet.add(next);
                Set<OperatorWrapper> selfIsMustSet = wrapper.getSelfIsMustSet();
                //从当前节点的强依赖集合中删除未选择的后继节点
                if (selfIsMustSet != null && selfIsMustSet.contains(next)) {
                    selfIsMustSet.remove(next);
                }
            }
            nextWrappers.removeAll(removeNextSet);
            removeNonChooseOps(removeNextSet, new HashSet<String>());
        } catch (Throwable e) {
            log.error("chooseOp error", e);
        }
    }

    /**
     * 递归待删除的分支节点，将计数器减一
     */
    private void removeNonChooseOps(Set<OperatorWrapper<?, ?>> removeOps, HashSet<String> idSet) {
        if (removeOps == null || removeOps.size() == 0) {
            return;
        }
        for (OperatorWrapper<?, ?> curr : removeOps) {
            //避免重复遍历
            if (idSet.contains(curr.getId())) {
                continue;
            }
            idSet.add(curr.getId());
            //计数器减一
            syncLatch.countDown();
            removeNonChooseOps(curr.getNextWrappers(), idSet);
        }
    }

    /**
     * 检查结束节点是否都已经执行完，如果执行完，需要将主线程唤醒
     */
    private void checkEndpointFinish() {
        if (!endpointSet.isEmpty()) {
            return;
        }
        state = DagState.FINISH;
        if (endpointLock.tryLock()) {
            try {
                while (syncLatch.getCount() >= 1) {
                    syncLatch.countDown();
                }
            } finally {
                endpointLock.unlock();
            }
        }
    }

    private void notifyNextWrappers(OperatorWrapper<?, ?> wrapper, Set<OperatorWrapper<?, ?>> nextWrappers) {
        if (nextWrappers == null) {
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
                syncLatch.countDown();

                this.interruptOrUpdateDependSkipState(depend);
            }
        }
    }

    /**
     * 调用目标operator的 execute 方法
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
     * @param wrapper
     * @return
     */
    private Object parseOperatorParam(OperatorWrapper wrapper) {
        Object param = null;
        //解析OP执行时需要透传的参数
        {
            List list = null;
            //参数来源是外部变量
            if (null != wrapper.getParamList()) {
                list = wrapper.getParamList();
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
        return param;
    }

    public ExecutorService getExecutor() {
        return this.executor;
    }

    public Map<String, OperatorWrapper<?, ?>> getWrapperMap() {
        return wrapperMap;
    }

    public ConcurrentHashMap.KeySetView<Thread, Boolean> getRunningThreadSet() {
        return runningThreadSet;
    }

    public Throwable getEx() {
        return ex;
    }

    public DagContext getDagContext() {
        return dagContext;
    }
}