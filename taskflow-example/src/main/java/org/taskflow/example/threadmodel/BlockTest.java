package org.taskflow.example.threadmodel;

import org.taskflow.core.DagEngine;
import org.taskflow.core.thread.pool.CustomThreadPool;
import org.taskflow.core.wrapper.OperatorWrapper;
import org.junit.Test;

import java.util.concurrent.ExecutorService;

/**
 * 线程模型--阻塞模式
 * Created by ytyht226 on 2022/6/23.
 */
public class BlockTest {
    ExecutorService executor = CustomThreadPool.newFixedThreadPoolWrapper(5);
    Operator1 operator1 = new Operator1();

    @Test
    public void test() {
        DagEngine engine = new DagEngine(executor);
        OperatorWrapper<Void, Integer> wrapper1 = new OperatorWrapper<Void, Integer>()
                .id("1")
                .engine(engine)
                .operator(operator1)
                ;

        engine.runAndWait(900_000);
        if (engine.getEx() != null) {
            engine.getEx().printStackTrace();
        }
    }
}