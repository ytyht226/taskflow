package org.taskflow.core.task;

import lombok.Data;
import org.taskflow.core.wrapper.OperatorWrapper;

import java.util.List;


/**
 * 任务入参及结果
 * 参数可以传入多个，比如批量查询接口
 * @param <P>   执行任务的入参类型
 * @param <V>   任务执行结果类型
 * Created by ytyht226 on 2023/3/17.
 */
@Data
public class MultiParamTaskResult<P, V> {
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
    private transient OperatorWrapper<List<P>, List<MultiParamTaskResult<P, V>>> operatorWrapper;

    public MultiParamTaskResult(P obj) {
        this.obj = obj;
    }

    public MultiParamTaskResult(P obj, V data) {
        this.obj = obj;
        this.data = data;
    }

    public MultiParamTaskResult(P obj, OperatorWrapper<List<P>, List<MultiParamTaskResult<P, V>>> operatorWrapper) {
        this.obj = obj;
        this.operatorWrapper = operatorWrapper;
    }

    public MultiParamTaskResult(P obj, V data, OperatorWrapper<List<P>, List<MultiParamTaskResult<P, V>>> operatorWrapper) {
        this.obj = obj;
        this.data = data;
        this.operatorWrapper = operatorWrapper;
    }
}