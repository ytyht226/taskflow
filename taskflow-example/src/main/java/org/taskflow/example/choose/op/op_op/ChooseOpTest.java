package org.taskflow.example.choose.op.op_op;

import com.google.common.collect.Sets;
import org.taskflow.core.DagEngine;
import org.taskflow.core.thread.pool.CustomThreadPool;
import org.taskflow.core.wrapper.OperatorWrapper;
import org.junit.Test;

import java.util.concurrent.ExecutorService;

/**
 * 节点选择
 * 待选择的都是节点
 * Created by ytyht226 on 2022/6/23.
 */
@SuppressWarnings("all")
public class ChooseOpTest {
    ExecutorService executor = CustomThreadPool.newFixedThreadPoolWrapper(5);
    Operator1 operator1 = new Operator1();
    Operator2 operator2 = new Operator2();
    Operator3 operator3 = new Operator3();
    Operator4 operator4 = new Operator4();

    @Test
    public void test() {
        DagEngine engine = new DagEngine(executor);
        OperatorWrapper<Integer, Integer> wrapper1 = new OperatorWrapper<Integer, Integer>()
                .id("1")
                .engine(engine)
                .operator(operator1)
                .next("2", "3")
                .chooseOp((w) -> {
                    return Sets.newHashSet("3");
                });
        OperatorWrapper<Integer, Integer> wrapper2 = new OperatorWrapper<Integer, Integer>()
                .id("2")
                .engine(engine)
                .operator(operator2)
                .next("4")
                ;
        OperatorWrapper<Integer, Integer> wrapper3 = new OperatorWrapper<Integer, Integer>()
                .id("3")
                .engine(engine)
                .operator(operator3)
                .next("4")
                ;
        OperatorWrapper<Integer, Integer> wrapper4 = new OperatorWrapper<Integer, Integer>()
                .id("4")
                .engine(engine)
                .operator(operator4)
                ;

        engine.runAndWait(300_000);
        if (engine.getEx() != null) {
            engine.getEx().printStackTrace();
        }
    }
}