package org.taskflow.core.operator;

import org.taskflow.core.wrapper.OperatorWrapper;

/**
 * Operator接口
 * Created by ytyht226 on 2022/3/16.
 */
@FunctionalInterface
public interface IOperator<P, V> {

    /**
     * 自定义OP的默认返回值，比如节点执行异常时
     */
    default V defaultValue() {
        return null;
    }

    /**
     * 该方法实现OP的具体处理逻辑
     */
    V execute(P param) throws Exception;

    /**
     * OP执行前回调
     */
    default void onStart(P param) {
    }
    /**
     * OP执行成功后回调
     */
    default void onSuccess(P param, OperatorResult<V> result) {
    }
    /**
     * OP执行异常后回调
     */
    default void onError(P param, OperatorResult<V> result) {
    }

}
