package org.taskflow.core.exception;

/**
 * 自定义运行时异常
 * Created by ytyht226 on 2022/3/16.
 */
public class TaskFlowException extends RuntimeException {

    public TaskFlowException(String message) {
        super(message);
    }

    public TaskFlowException(Exception e) {
        super(e);
    }
}