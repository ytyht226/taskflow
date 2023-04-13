package org.taskflow.example.threadmodel;

import org.taskflow.core.DagEngine;
import org.taskflow.core.thread.pool.CustomThreadPool;
import org.taskflow.core.wrapper.OperatorWrapper;
import org.junit.Test;

import java.util.concurrent.ExecutorService;

/**
 * 线程模型--非阻塞模式
 * Created by ytyht226 on 2022/6/23.
 */
public class NonBlockTest {
    ExecutorService executor = CustomThreadPool.newFixedThreadPoolWrapper(5);
    private final Operator1 operator1 = new Operator1();

    @Test
    public void test() {
        DagEngine engine = new DagEngine(executor);
        OperatorWrapper<Void, Integer> wrapper1 = new OperatorWrapper<Void, Integer>()
                .id("1")
                .engine(engine)
                .operator(operator1)
                ;

        engine.runWithCallback(900_000, this::engineCallback);

        System.out.println("main end...");

        synchronized (operator1) {
            try {
                operator1.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void engineCallback() {
        System.out.println("engine callback...");
        synchronized (operator1) {
            operator1.notify();
        }
    }
}