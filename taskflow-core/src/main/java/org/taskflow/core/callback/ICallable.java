package org.taskflow.core.callback;

import org.taskflow.core.wrapper.OperatorWrapper;

/**
 * OP执行前后的回调接口
 * Created by ytyht226 on 2022/4/8.
 */
@SuppressWarnings("rawtypes")
@FunctionalInterface
public interface ICallable {

    void call(OperatorWrapper wrapper);

}
