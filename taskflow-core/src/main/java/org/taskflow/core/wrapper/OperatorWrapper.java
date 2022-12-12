package org.taskflow.core.wrapper;

import org.taskflow.core.DagEngine;
import org.taskflow.core.callback.ICallable;
import org.taskflow.core.callback.IChoose;
import org.taskflow.core.callback.ICondition;
import org.taskflow.core.enums.WrapperState;
import org.taskflow.core.event.OperatorEventEnum;
import org.taskflow.core.exception.TaskFlowException;
import org.taskflow.core.listener.OperatorListener;
import org.taskflow.core.operator.DefaultParamParseOperator;
import org.taskflow.core.operator.IOperator;
import org.taskflow.core.operator.OperatorResult;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * OP节点的包装类
 * Created by ytyht226 on 2022/3/16.
 */
public class OperatorWrapper<P, V> {
    /**
     * OP入参解析器（json-path 方式解析）
     */
    private static final DefaultParamParseOperator DEFAULT_PARAM_PARSE_OPERATOR = new DefaultParamParseOperator();
    /**
     * 该wrapper的id，如果不指定，默认是Operator的全限定名
     */
    private String id;
    /**
     * 参数来源是请求上下文，context 和 paramFromList都设置时，context 参数在前面
     * 集合中的元素与参数顺序一致
     */
    private Object context;
    /**
     * 参数来源是依赖的OP的结果
     * 集合中的元素与参数顺序一致
     */
    private List<String> paramFromList;
    /**
     * OP节点配置
     * @see org.taskflow.config.op.OpConfig
     */
    private String opConfig;
    /**
     * 通过 json-path 方式解析参数时要执行的目标对象（具体执行的方法通过 opConfig 定义）
     */
    private Object proxyObj;
    /**
     * 该wrapper代理的目标OP
     */
    private IOperator<P, V> operator;
    /**
     * 该OP的后继OP集合
     */
    private Set<OperatorWrapper<?, ?>> nextWrappers;
    /**
     * 该OP的后继OP集合id
     */
    private Map<String /*nextWrapperId*/, Boolean /*后继节点是否强依赖该节点*/> nextWrapperIdMap;
    /**
     * 该OP依赖的前驱OP集合
     */
    private Set<OperatorWrapper<?, ?>> dependWrappers;
    /**
     * 该OP依赖的前驱OP集合id
     */
    private Map<String /*dependWrapperId*/, Boolean /*当前节点是否强依赖前驱节点*/> dependWrapperIdMap;
    /**
     * 强依赖于该OP的后继 wrapper 集合，是 nextWrappers 的子集
     */
    private Set<OperatorWrapper<?, ?>> selfIsMustSet;
    /**
     * 节点的入度，不同于常规的定义，这里的入度只计算强依赖的节点，当 indegree=0 时，当前OP才能执行，在一个编排流程中，一定满足如下条件
     * indegree <= dependWrappers.size()
     */
    private AtomicInteger indegree = new AtomicInteger(0);
    /**
     * OP返回的结果
     */
    private volatile OperatorResult<V> operatorResult = OperatorResult.defaultResult();
    /**
     * 当前节点执行的状态
     */
    private AtomicInteger wrapperState = new AtomicInteger(WrapperState.INIT);
    /**
     * 执行该节点的线程
     */
    private Thread thread;
    /**
     * 绑定的DAG执行引擎
     */
    private DagEngine engine;
    /**
     * 是否已经初始化，只初始化一次
     */
    private boolean init;
    /**
     * OP执行时动态解析的参数，合并 paramList、paramFromList 之后的结果
     */
    private P param;
    /**
     * 条件判断，根据前驱节点的结果判断是否可以执行该节点
     * 注意：依赖的节点全都是强依赖时，不起作用，主要使用在弱依赖的场景
     * 比如：1 依赖 2、3、4，节点1执行的条件是2、3、4中执行完的个数 >=2, 此时可以通过条件判断，如果不满足条件时返回false
     */
    private ICondition condition;
    /**
     * 分支选择，根据当前节点的结果判断要执行的后继节点
     */
    private IChoose choose;
    /**
     * OP执行过程的监听器
     */
    private Map<OperatorEventEnum, List<OperatorListener>> listenerEventMap;
    /**
     * OP执行前的回调
     */
    private ICallable before;
    /**
     * OP执行后的回调
     */
    private ICallable after;

    public OperatorWrapper(){

    }
    public OperatorWrapper(String id, IOperator<P, V> operator) {
        if (operator == null) {
            throw new TaskFlowException("operator is null");
        }
        this.id = id;
        this.operator = operator;
    }

    public OperatorWrapper<P, V> addParamFromWrapperId(String... fromWrapperIds) {
        if (fromWrapperIds == null) {
            return this;
        }
        if (paramFromList == null || paramFromList.size() == 0) {
            paramFromList = new ArrayList<>();
        }
        paramFromList.addAll(Arrays.asList(fromWrapperIds));
        this.depend(fromWrapperIds);
        return this;
    }

    public OperatorWrapper<P, V> id(String id) {
        this.id = id;
        return this;
    }

    public OperatorWrapper<P, V> operator(IOperator<P, V> operator) {
        this.operator = operator;
        if (this.id == null) {
            this.id = operator.getClass().getCanonicalName();
        }
        return this;
    }

    public OperatorWrapper<P, V> engine(DagEngine engine) {
        if (engine == null) {
            throw new TaskFlowException("DagEngine is null");
        }
        if (id == null) {
            throw new TaskFlowException("id is null");
        }
        if (engine.getWrapperMap().containsKey(id)) {
            throw new TaskFlowException("id duplicates");
        }
        engine.getWrapperMap().put(id, this);
        this.engine = engine;
        return this;
    }

    public OperatorWrapper<P, V> condition(ICondition condition) {
        this.condition = condition;
        return this;
    }

    public OperatorWrapper<P, V> before(ICallable before) {
        this.before = before;
        return this;
    }

    public OperatorWrapper<P, V> after(ICallable after) {
        this.after = after;
        return this;
    }

    public OperatorWrapper<P, V> chooseNext(IChoose choose) {
        this.choose = choose;
        return this;
    }

    public OperatorWrapper<P, V> depend(String... wrapperIds) {
        if (wrapperIds == null) {
            return this;
        }
        for (String wrapperId : wrapperIds) {
            this.depend(wrapperId, true);
        }
        return this;
    }

    public OperatorWrapper<P, V> depend(String wrapperId, boolean isMust) {
        if (wrapperId == null) {
            return this;
        }
        if (dependWrapperIdMap == null) {
            dependWrapperIdMap = new HashMap<>();
        }
        dependWrapperIdMap.put(wrapperId, isMust);
        return this;
    }

    public OperatorWrapper<P, V> next(String... wrapperIds) {
        if (wrapperIds == null) {
            return this;
        }
        for (String wrapperId : wrapperIds) {
            this.next(wrapperId, true);
        }
        return this;
    }

    public OperatorWrapper<P, V> next(String wrapperId, boolean selfIsMust) {
        if (wrapperId == null) {
            return this;
        }
        if (nextWrapperIdMap == null) {
            nextWrapperIdMap = new HashMap<>();
        }
        nextWrapperIdMap.put(wrapperId, selfIsMust);
        return this;
    }

    public OperatorWrapper<P, V> context(Object context) {
        this.context = context;
        return this;
    }

    public OperatorWrapper<P, V> init() {
        if (init) {
            return this;
        }
        this.init = true;
        return this;
    }

    public boolean compareAndSetState(int expect, int update) {
        return this.wrapperState.compareAndSet(expect, update);
    }

    public OperatorWrapper<P, V> addListener(OperatorListener listener, OperatorEventEnum eventEnum) {
        if (listenerEventMap == null) {
            listenerEventMap = new HashMap<>();

        }
        if (!listenerEventMap.containsKey(eventEnum)) {
            listenerEventMap.put(eventEnum, new ArrayList<>());
        }
        listenerEventMap.get(eventEnum).add(listener);
        return this;
    }

    public List<OperatorListener> getListener(OperatorEventEnum eventEnum) {
        return listenerEventMap == null ? null : listenerEventMap.get(eventEnum);
    }

    @Override
    public String toString() {
        return "OperatorWrapper{" +
                "id='" + id + '\'' +
                ", operator=" + operator +
                '}';
    }

    public String getId() {
        return id;
    }

    public AtomicInteger getIndegree() {
        return indegree;
    }

    public IOperator<P, V> getOperator() {
        return operator;
    }

    public OperatorResult<V> getOperatorResult() {
        return operatorResult;
    }

    public AtomicInteger getWrapperState() {
        return wrapperState;
    }

    public Set<OperatorWrapper<?, ?>> getNextWrappers() {
        return nextWrappers;
    }

    public void setNextWrappers(Set<OperatorWrapper<?, ?>> nextWrappers) {
        this.nextWrappers = nextWrappers;
    }

    public Set<OperatorWrapper<?, ?>> getDependWrappers() {
        return dependWrappers;
    }

    public void setDependWrappers(Set<OperatorWrapper<?, ?>> dependWrappers) {
        this.dependWrappers = dependWrappers;
    }

    public Set<OperatorWrapper<?, ?>> getSelfIsMustSet() {
        return selfIsMustSet;
    }

    public void setSelfIsMustSet(Set<OperatorWrapper<?, ?>> selfIsMustSet) {
        this.selfIsMustSet = selfIsMustSet;
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }

    public Thread getThread() {
        return thread;
    }

    public List<String> getParamFromList() {
        return paramFromList;
    }

    public void setEngine(DagEngine engine) {
        this.engine = engine;
        this.engine.getWrapperMap().put(this.getId(), this);
    }

    public Object getContext() {
        return context;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isInit() {
        return init;
    }

    public void setInit(boolean init) {
        this.init = init;
    }

    public P getParam() {
        return param;
    }

    public void setParam(P param) {
        this.param = param;
    }

    public Map<String, Boolean> getNextWrapperIdMap() {
        return nextWrapperIdMap;
    }

    public Map<String, Boolean> getDependWrapperIdMap() {
        return dependWrapperIdMap;
    }

    public ICondition getCondition() {
        return condition;
    }

    public DagEngine getEngine() {
        return engine;
    }

    public IChoose getChoose() {
        return choose;
    }

    public ICallable getBefore() {
        return before;
    }

    public ICallable getAfter() {
        return after;
    }

    public OperatorWrapper<P, V> proxyObj(Object proxyObj) {
        this.proxyObj = proxyObj;
        return this;
    }

    @SuppressWarnings("all")
    public OperatorWrapper<P, V> opConfig(String opConfig) {
        return this.opConfig(opConfig, (IOperator<P, V>) DEFAULT_PARAM_PARSE_OPERATOR);
    }

    public OperatorWrapper<P, V> opConfig(String opConfig, IOperator<P, V> operator) {
        this.opConfig = opConfig;
        this.operator = operator;
        this.context(this);
        return this;
    }

    public String getOpConfig() {
        return opConfig;
    }

    public Object getProxyObj() {
        return proxyObj;
    }
}