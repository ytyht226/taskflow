package org.taskflow.core.context;

import org.taskflow.core.operator.OperatorResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DAG执行引擎上下文
 * 上下文的生命周期是引擎执行期间，即从开始节点到结束节点之间
 * Created by ytyht226 on 2022/3/23.
 */
@SuppressWarnings("rawtypes")
public class DagContext {
    /**
     * 保存每个Operator返回的结果
     */
    private Map<String /* wrapperId */, OperatorResult> operatorResultMap = new ConcurrentHashMap<>();

    public DagContext() {

    }

    public void putOperatorResult(String wrapperId, OperatorResult<?> operatorResult) {
        operatorResultMap.put(wrapperId, operatorResult);
    }

    public OperatorResult getOperatorResult(String wrapperId) {
        return operatorResultMap.get(wrapperId);
    }

}