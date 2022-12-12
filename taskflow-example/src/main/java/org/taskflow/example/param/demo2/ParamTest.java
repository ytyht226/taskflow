package org.taskflow.example.param.demo2;

import org.junit.Test;
import org.taskflow.config.op.OpConfig;
import org.taskflow.core.DagEngine;
import org.taskflow.core.wrapper.OperatorWrapper;
import org.taskflow.example.param.entity.OpConfigEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 节点参数来源
 * 开始节点使用上下文，其它节点入参使用依赖op的返回结果（依赖多个）
 * 执行顺序：1 -> (2、3) -> 4
 * Created by ytyht226 on 2022/6/23.
 */
@SuppressWarnings("all")
public class ParamTest {
    Operator1 operator1 = new Operator1();
    Operator2 operator2 = new Operator2();
    Operator3 operator3 = new Operator3();
    Operator4 operator4 = new Operator4();
    ExecutorService executor = Executors.newFixedThreadPool(5);

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
        OperatorWrapper wrapper2 = new OperatorWrapper<String, OpConfig>()
                .id("2")
                .engine(engine)
                .operator(operator2)
                .addParamFromWrapperId("1")    //参数来源是其它节点的结果
                ;
        OperatorWrapper wrapper3 = new OperatorWrapper<String, OpConfig>()
                .id("3")
                .engine(engine)
                .operator(operator3)
                .addParamFromWrapperId("1")    //参数来源是其它节点的结果
                ;

        OperatorWrapper wrapper4 = new OperatorWrapper<List<OpConfig>, List<OpConfig>>()
                .id("4")
                .engine(engine)
                .operator(operator4)
                .addParamFromWrapperId("2", "3")    //参数来源是其它节点的结果
                ;

        engine.runAndWait(3000);
        if (engine.getEx() != null) {
            engine.getEx().printStackTrace();
        }
        System.out.println(wrapper4.getOperatorResult().getResult());
    }
}