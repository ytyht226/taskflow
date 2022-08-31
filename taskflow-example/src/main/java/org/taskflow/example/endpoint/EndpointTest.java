package org.taskflow.example.endpoint;

import org.junit.Test;
import org.taskflow.core.DagEngine;
import org.taskflow.core.wrapper.OperatorWrapper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 自定义流程中断
 * Created by ytyht226 on 2022/6/23.
 */
@SuppressWarnings("all")
public class EndpointTest {
    Operator1 operator1 = new Operator1();
    Operator2 operator2 = new Operator2();
    Operator3 operator3 = new Operator3();
    ExecutorService executor = Executors.newFixedThreadPool(5);

    @Test
    public void test() {
        DagEngine engine = new DagEngine(executor);
        OperatorWrapper<Integer, Integer> wrapper1 = new OperatorWrapper<Integer, Integer>()
                .id("1")
                .engine(engine)
                .operator(operator1)
                .next("2")
                ;
        OperatorWrapper<Integer, Integer> wrapper2 = new OperatorWrapper<Integer, Integer>()
                .id("2")
                .engine(engine)
                .operator(operator2)
                .next("3")
                .after((w) -> {
                    //将当前节点设置为结束节点，后续节点不再执行
                    DagEngine.stopAt(w);
                })
                ;
        OperatorWrapper<Integer, Integer> wrapper3 = new OperatorWrapper<Integer, Integer>()
                .id("3")
                .engine(engine)
                .operator(operator3)
                ;

        engine.runAndWait(3000);
    }
}