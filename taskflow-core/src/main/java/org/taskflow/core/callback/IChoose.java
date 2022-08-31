package org.taskflow.core.callback;

import org.taskflow.core.wrapper.OperatorWrapper;

import java.util.Set;

/**
 * 分支选择接口，引擎执行过程中，动态的选择当前节点要执行的后继节点
 * Created by ytyht226 on 2022/6/24.
 */
@SuppressWarnings("rawtypes")
@FunctionalInterface
public interface IChoose {
    Set<String> choose(OperatorWrapper wrapper);
}
