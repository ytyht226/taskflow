package org.taskflow.core.enums;

/**
 * 节点运行状态枚举
 * Created by ytyht226 on 2022/3/17.
 */
public interface WrapperState {
    /**
     * 初始状态
     */
    int INIT = 0;
    /**
     * 执行中
     */
    int RUNNING = 1;
    /**
     * 执行结束
     */
    int FINISH = 2;
    /**
     * 节点执行异常
     */
    int ERROR = 3;
    /**
     * 跳过当前节点
     */
    int SKIP = 4;
}
