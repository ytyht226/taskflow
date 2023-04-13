package org.taskflow.example.choose.branch;

import com.google.common.collect.Sets;
import org.taskflow.core.DagEngine;
import org.taskflow.core.thread.pool.CustomThreadPool;
import org.taskflow.core.wrapper.OperatorWrapper;
import org.junit.Test;

import java.util.concurrent.ExecutorService;

/**
 * 分支选择
 * Created by ytyht226 on 2022/6/23.
 */
@SuppressWarnings("all")
public class ChooseBranchTest {
    ExecutorService executor = CustomThreadPool.newFixedThreadPoolWrapper(5);
    Operator1 operator1 = new Operator1();
    Operator2 operator2 = new Operator2();
    Operator3 operator3 = new Operator3();
    Operator4 operator4 = new Operator4();
    Operator5 operator5 = new Operator5();
    Operator6 operator6 = new Operator6();

    @Test
    public void test() {
        DagEngine engine = new DagEngine(executor);
        OperatorWrapper<Integer, Integer> wrapper1 = new OperatorWrapper<Integer, Integer>()
                .id("1")
                .engine(engine)
                .operator(operator1)
                .chooseBranch((w) -> {
                    return Sets.newHashSet("2");
                })
                ;
        OperatorWrapper<Integer, Integer> wrapper2 = new OperatorWrapper<Integer, Integer>()
                .id("2")
                .engine(engine)
                .operator(operator2)
                .depend("1")
                ;
        OperatorWrapper<Integer, Integer> wrapper3 = new OperatorWrapper<Integer, Integer>()
                .id("3")
                .engine(engine)
                .operator(operator3)
                .depend("1")
                ;
        OperatorWrapper<Integer, Integer> wrapper4 = new OperatorWrapper<Integer, Integer>()
                .id("4")
                .engine(engine)
                .operator(operator4)
                .depend("1")
                ;
        OperatorWrapper<Integer, Integer> wrapper5 = new OperatorWrapper<Integer, Integer>()
                .id("5")
                .engine(engine)
                .operator(operator5)
                .depend("2")
                ;
        OperatorWrapper<Integer, Integer> wrapper6 = new OperatorWrapper<Integer, Integer>()
                .id("6")
                .engine(engine)
                .operator(operator6)
                .depend("2")
                ;

        engine.runAndWait(300_000);
        if (engine.getEx() != null) {
            engine.getEx().printStackTrace();
        }
    }
}