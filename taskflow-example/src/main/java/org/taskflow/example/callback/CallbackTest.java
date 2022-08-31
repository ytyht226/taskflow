package org.taskflow.example.callback;

import org.junit.Test;
import org.taskflow.core.DagEngine;
import org.taskflow.core.wrapper.OperatorWrapper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 回调接口：引擎回调、OP回调
 * Created by ytyht226 on 2022/6/23.
 */
public class CallbackTest {
    Operator1 operator1 = new Operator1();
    ExecutorService executor = Executors.newFixedThreadPool(5);

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
                ;

        engine.runAndWait(9000);
    }
}