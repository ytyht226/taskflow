package org.taskflow.core.task;

import lombok.Data;
import org.taskflow.core.wrapper.OperatorWrapper;


/**
 * 任务入参及结果
 * @param <P>   执行任务的入参类型
 * @param <V>   任务执行结果类型
 * Created by ytyht226 on 2023/3/17.
 */
@Data
public class TaskResult<P, V> {
    /**
     * 任务入参
     */
    private P obj;
    /**
     * 任务结果
     */
    private V data;
    /**
     * 任务执行状态，data为空时，可以查看执行的信息
     */
    private transient OperatorWrapper<P, V> operatorWrapper;

    public TaskResult(P obj) {
        this.obj = obj;
    }

    public TaskResult(P obj, V data) {
        this.obj = obj;
        this.data = data;
    }

    public TaskResult(P obj, OperatorWrapper<P, V> operatorWrapper) {
        this.obj = obj;
        this.operatorWrapper = operatorWrapper;
    }

    public TaskResult(P obj, V data, OperatorWrapper<P, V> operatorWrapper) {
        this.obj = obj;
        this.data = data;
        this.operatorWrapper = operatorWrapper;
    }
}