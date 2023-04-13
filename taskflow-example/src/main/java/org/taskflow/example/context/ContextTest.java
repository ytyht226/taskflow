package org.taskflow.example.context;

import org.taskflow.core.DagEngine;
import org.taskflow.core.thread.pool.CustomThreadPool;
import org.taskflow.core.wrapper.OperatorWrapper;
import org.junit.Test;

import java.util.concurrent.ExecutorService;

/**
 * 全局上下文
 * Created by ytyht226 on 2022/6/23.
 */
public class ContextTest {
    ExecutorService executor = CustomThreadPool.newFixedThreadPoolWrapper(5);
    Operator1 operator1 = new Operator1();
    Operator2 operator2 = new Operator2();

    @Test
    public void test() {
        DagEngine engine = new DagEngine(executor);
        OperatorWrapper<Void, Integer> wrapper1 = new OperatorWrapper<Void, Integer>()
                .id("1")
                .engine(engine)
                .operator(operator1)
                ;
        OperatorWrapper<Void, Integer> wrapper2 = new OperatorWrapper<Void, Integer>()
                .id("2")
                .engine(engine)
                .operator(operator2)
                .depend("1")
                ;
        engine.runAndWait(9000);
        if (engine.getEx() != null) {
            engine.getEx().printStackTrace();
        }
    }
}