package org.taskflow.example.param.demo;

import org.junit.Test;
import org.taskflow.common.util.gson.GsonUtil;
import org.taskflow.config.op.OpConfig;
import org.taskflow.core.DagEngine;
import org.taskflow.core.thread.pool.CustomThreadPool;
import org.taskflow.core.wrapper.OperatorWrapper;
import org.taskflow.example.param.entity.OpConfigEntity;

import java.util.concurrent.ExecutorService;

/**
 * 节点参数来源
 * 不设置op参数，默认使用请求上下文
 * 执行顺序：1 -> 2 -> 3
 * Created by ytyht226 on 2022/6/23.
 */
@SuppressWarnings("all")
public class ParamTest {
    ExecutorService executor = CustomThreadPool.newFixedThreadPoolWrapper(5);
    Operator1 operator1 = new Operator1();
    Operator2 operator2 = new Operator2();
    Operator3 operator3 = new Operator3();

    @Test
    public void test() {
        //请求上下文
        OpConfig opConfig = OpConfigEntity.getOpConfig();
        System.out.println("before: " + GsonUtil.prettyPrint(opConfig));
        DagEngine engine = new DagEngine(opConfig, executor);
        OperatorWrapper wrapper1 = new OperatorWrapper<OpConfig, OpConfig>()
                .id("1")
                .engine(engine)
                .operator(operator1)
                .next("2")
                ;
        OperatorWrapper wrapper2 = new OperatorWrapper<OpConfig, OpConfig>()
                .id("2")
                .engine(engine)
                .operator(operator2)
                .next("3")
                ;
        OperatorWrapper wrapper3 = new OperatorWrapper<OpConfig, OpConfig>()
                .id("3")
                .engine(engine)
                .operator(operator3)
                ;

        engine.runAndWait(300_000);
        System.out.println("after: " + GsonUtil.prettyPrint(opConfig));
        if (engine.getEx() != null) {
            engine.getEx().printStackTrace();
        }
    }
}