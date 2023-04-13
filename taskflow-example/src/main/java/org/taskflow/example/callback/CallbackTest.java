package org.taskflow.example.callback;

import org.taskflow.core.DagEngine;
import org.taskflow.core.thread.pool.CustomThreadPool;
import org.taskflow.core.wrapper.OperatorWrapper;
import org.junit.Test;

import java.util.concurrent.ExecutorService;

/**
 * 回调接口：引擎回调、OP回调
 * Created by ytyht226 on 2022/6/23.
 */
public class CallbackTest {
    ExecutorService executor = CustomThreadPool.newFixedThreadPoolWrapper(5);
    Operator1 operator1 = new Operator1();
    Operator2 operator2 = new Operator2();

    @Test
    public void test() {
        DagEngine engine = new DagEngine(executor);
        //引擎回调
        engine.before(() -> {
            System.out.println("engine start...");
        });
        engine.after(() -> {
            System.out.println("engine end...");
        });
        //每个OP的回调
        engine.beforeOp((w) -> {
            System.out.println("OP: " + w.getId() + " start...");
        });
        engine.afterOp((w) -> {
            System.out.println("OP: " + w.getId() + " end...");
        });
        OperatorWrapper<Void, Integer> wrapper1 = new OperatorWrapper<Void, Integer>()
                .id("1")
                .engine(engine)
                .operator(operator1)
                .next("2")
                ;

        OperatorWrapper<Void, Integer> wrapper2 = new OperatorWrapper<Void, Integer>()
                .id("2")
                .engine(engine)
                .operator(operator2)
                ;

        engine.runAndWait(9000);
        if (engine.getEx() != null) {
            engine.getEx().printStackTrace();
        }
    }
}