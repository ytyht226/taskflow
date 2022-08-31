package org.taskflow.example.param;

import org.junit.Test;
import org.taskflow.core.DagEngine;
import org.taskflow.core.wrapper.OperatorWrapper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 节点参数来源
 * Created by ytyht226 on 2022/6/23.
 */
@SuppressWarnings("all")
public class ParamTest {
    Operator1 operator1 = new Operator1();
    Operator2 operator2 = new Operator2();
    Operator3 operator3 = new Operator3();
    ExecutorService executor = Executors.newFixedThreadPool(5);

    @Test
    public void test() {
        int param = 5;
        DagEngine engine = new DagEngine(executor);
        OperatorWrapper<Integer, Integer> wrapper1 = new OperatorWrapper<Integer, Integer>()
                .id("1")
                .engine(engine)
                .operator(operator1)
                .next("2")
                .addParam(param)    //参数来源是外部变量
                ;
        OperatorWrapper<Integer, Integer> wrapper2 = new OperatorWrapper<Integer, Integer>()
                .id("2")
                .engine(engine)
                .operator(operator2)
                .next("3")
                .addParamFromWrapperId("1")    //参数来源是其它节点的结果
                ;
        OperatorWrapper<Integer, Integer> wrapper3 = new OperatorWrapper<Integer, Integer>()
                .id("3")
                .engine(engine)
                .operator(operator3)
                .addParamFromWrapperId("2")    //参数来源是其它节点的结果
                ;

        engine.runAndWait(3000);
    }
}