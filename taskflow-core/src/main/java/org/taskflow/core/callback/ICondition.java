package org.taskflow.core.callback;

import org.taskflow.core.wrapper.OperatorWrapper;

/**
 * 准入条件判断接口，引擎执行过程中，动态的判断是否满足执行该节点的条件
 * Created by ytyht226 on 2022/4/8.
 */
@SuppressWarnings("rawtypes")
@FunctionalInterface
public interface ICondition {

    /**
     * 判断是否可以执行当前节点
     */
    boolean judge(OperatorWrapper wrapper);

}
