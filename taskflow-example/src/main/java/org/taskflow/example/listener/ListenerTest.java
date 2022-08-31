package org.taskflow.example.listener;

import org.junit.Test;
import org.taskflow.core.DagEngine;
import org.taskflow.core.event.OperatorEventEnum;
import org.taskflow.core.listener.OperatorListener;
import org.taskflow.core.wrapper.OperatorWrapper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 节点执行状态监听器
 * Created by ytyht226 on 2022/3/17.
 */
public class ListenerTest {
    Operator1 operator1 = new Operator1();
    Operator2 operator2 = new Operator2();
    Operator3 operator3 = new Operator3();
    ExecutorService executor = Executors.newFixedThreadPool(5);

    @Test
    public void test() {
        OperatorListener listener1 = getListener();
        DagEngine engine = new DagEngine(executor);
        OperatorWrapper<Integer, Integer> wrapper1 = new OperatorWrapper<Integer, Integer>()
                .id("1")
                .engine(engine)
                .operator(operator1)
                //给节点1添加三种执行状态的监听器，开始、异常、正常结束
                .addListener(listener1, OperatorEventEnum.START)
                .addListener(listener1, OperatorEventEnum.ERROR)
                .addListener(listener1, OperatorEventEnum.SUCCESS)
                ;
        OperatorWrapper<Integer, Integer> wrapper2 = new OperatorWrapper<Integer, Integer>()
                .id("2")
                .engine(engine)
                .operator(operator2)
                .addParamFromWrapperId("1")
                ;
        OperatorWrapper<Integer, Integer> wrapper3 = new OperatorWrapper<Integer, Integer>()
                .id("3")
                .engine(engine)
                .operator(operator3)
                .addParamFromWrapperId("2")
                ;

        engine.runAndWait(900000);
    }

    /**
     * 不同的监听器状态对应的处理逻辑
     */
    private OperatorListener getListener() {
        return (wrapper, eventEnum) -> {
            if (eventEnum == OperatorEventEnum.START) {
                System.out.println("Op1 start...");
            }
            if (eventEnum == OperatorEventEnum.SUCCESS) {
                System.out.println("Op1 success...");
            }
            if (eventEnum == OperatorEventEnum.ERROR) {
                System.out.println("Op1 error...");
            }
        };
    }
}