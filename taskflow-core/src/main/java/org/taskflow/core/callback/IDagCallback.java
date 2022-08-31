package org.taskflow.core.callback;

/**
 * 引擎执行前后的回调接口
 * Created by ytyht226 on 2022/3/24.
 */
@FunctionalInterface
public interface IDagCallback {
    void callback();
}