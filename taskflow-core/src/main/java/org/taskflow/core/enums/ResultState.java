package org.taskflow.core.enums;

/**
 * 节点执行结果状态枚举
 * Created by ytyht226 on 2022/3/17.
 */
public enum  ResultState {
    /**
     * 成功
     */
    SUCCESS,
    /**
     * 超时
     */
    TIMEOUT,
    /**
     * 异常
     */
    EXCEPTION,
    /**
     * 默认
     */
    DEFAULT
}