package org.taskflow.core.operator;

import org.taskflow.core.enums.ResultState;

/**
 * Operator执行结果
 * Created by ytyht226 on 2022/3/17.
 */
public class OperatorResult<V> {
    /**
     * 执行的结果
     */
    private V result;
    /**
     * 结果状态
     */
    private ResultState resultState;
    /**
     * 异常信息
     */
    private Throwable ex;

    public OperatorResult(V result, ResultState resultState) {
        this(result, resultState, null);
    }

    public OperatorResult(V result, ResultState resultState, Exception ex) {
        this.result = result;
        this.resultState = resultState;
        this.ex = ex;
    }

    public static <V> OperatorResult<V> defaultResult() {
        return new OperatorResult<>(null, ResultState.DEFAULT);
    }

    @Override
    public String toString() {
        return "OperatorResult{" +
                "result=" + result +
                ", resultState=" + resultState +
                ", ex=" + ex +
                '}';
    }

    public Throwable getEx() {
        return ex;
    }

    public void setEx(Throwable ex) {
        this.ex = ex;
    }

    public V getResult() {
        return result;
    }

    public void setResult(V result) {
        this.result = result;
    }

    public ResultState getResultState() {
        return resultState;
    }

    public void setResultState(ResultState resultState) {
        this.resultState = resultState;
    }
}