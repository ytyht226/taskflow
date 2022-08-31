package org.taskflow.core.listener;

import org.taskflow.core.event.OperatorEventEnum;
import org.taskflow.core.wrapper.OperatorWrapper;

/**
 * OP执行过程的监听器
 * Created by ytyht226 on 2022/6/21.
 */
@SuppressWarnings("rawtypes")
@FunctionalInterface
public interface OperatorListener {

    void onEvent(OperatorWrapper wrapper, OperatorEventEnum eventEnum);
}