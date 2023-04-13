package org.taskflow.example.param.demo1;

import org.junit.Test;
import org.taskflow.config.op.OpConfig;
import org.taskflow.core.DagEngine;
import org.taskflow.core.thread.pool.CustomThreadPool;
import org.taskflow.core.wrapper.OperatorWrapper;
import org.taskflow.example.param.entity.OpConfigEntity;

import java.util.concurrent.ExecutorService;

/**
 * 节点参数来源
 * 开始节点使用上下文，其它节点入参使用依赖op的返回结果
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
        DagEngine engine = new DagEngine(executor);
        OperatorWrapper wrapper1 = new OperatorWrapper<OpConfig, String>()
                .id("1")
                .engine(engine)
                .operator(operator1)
                .next("2")
                .context(opConfig) //参数来源是外部变量（请求上下文）
                ;
        OperatorWrapper wrapper2 = new OperatorWrapper<String, Object>()
                .id("2")
                .engine(engine)
                .operator(operator2)
                .next("3")
                .addParamFromWrapperId("1")    //参数来源是其它节点的结果
                ;
        OperatorWrapper wrapper3 = new OperatorWrapper<Object, OpConfig>()
                .id("3")
                .engine(engine)
                .operator(operator3)
                .addParamFromWrapperId("2")    //参数来源是其它节点的结果
                ;

        engine.runAndWait(300_000);
        System.out.println(wrapper3.getOperatorResult().getResult());
        if (engine.getEx() != null) {
            engine.getEx().printStackTrace();
        }
    }
}