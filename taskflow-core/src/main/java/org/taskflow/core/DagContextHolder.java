package org.taskflow.core;

import com.alibaba.ttl.TransmittableThreadLocal;
import org.taskflow.core.context.DagContext;
import org.taskflow.core.operator.OperatorResult;

/**
 * 获取DagContext上下文的工具类
 * Created by ytyht226 on 2022/3/23.
 */
@SuppressWarnings("rawtypes")
public class DagContextHolder {
    private static ThreadLocal<DagContext> holder = new TransmittableThreadLocal<>();

    protected static void set(DagContext dagContext) {
        holder.set(dagContext);
    }

    public static DagContext get() {
        return holder.get();
    }

    protected static void remove() {
        holder.remove();
    }


    public static void putOperatorResult(String wrapperId, OperatorResult<?> operatorResult) {
        holder.get().putOperatorResult(wrapperId, operatorResult);
    }

    public static OperatorResult getOperatorResult(String wrapperId) {
        return holder.get().getOperatorResult(wrapperId);
    }

}