package org.taskflow.example.dependencytype;

import org.junit.Test;
import org.taskflow.core.DagEngine;
import org.taskflow.core.wrapper.OperatorWrapper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 节点依赖类型 强依赖、弱依赖
 * Created by ytyht226 on 2022/6/23.
 */
@SuppressWarnings("all")
public class DependencyTypeTest {
    Operator1 operator1 = new Operator1();
    Operator2 operator2 = new Operator2();
    Operator3 operator3 = new Operator3();
    Operator4 operator4 = new Operator4();
    ExecutorService executor = Executors.newFixedThreadPool(5);

    /**
     * 强依赖
     */
    @Test
    public void strongTypeTest() {
        DagEngine engine = new DagEngine(executor);
        OperatorWrapper<Integer, Integer> wrapper1 = new OperatorWrapper<Integer, Integer>()
                .id("1")
                .engine(engine)
                .operator(operator1)
                .next("2", "3")
                ;
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

        engine.runAndWait(3000);
    }

    /**
     * 弱依赖
     * 节点4弱依赖2、3，只要2、3 中有任意一个执行完，就可以执行节点4
     */
    @Test
    public void weakTypeTest() {
        DagEngine engine = new DagEngine(executor);
        OperatorWrapper<Integer, Integer> wrapper1 = new OperatorWrapper<Integer, Integer>()
                .id("1")
                .engine(engine)
                .operator(operator1)
                .next("2", "3")
                ;
        OperatorWrapper<Integer, Integer> wrapper2 = new OperatorWrapper<Integer, Integer>()
                .id("2")
                .engine(engine)
                .operator(operator2)
                .next("4", false)
                ;
        OperatorWrapper<Integer, Integer> wrapper3 = new OperatorWrapper<Integer, Integer>()
                .id("3")
                .engine(engine)
                .operator(operator3)
                .next("4", false)
                ;

        OperatorWrapper<Integer, Integer> wrapper4 = new OperatorWrapper<Integer, Integer>()
                .id("4")
                .engine(engine)
                .operator(operator4)
                ;

        engine.runAndWait(3000);
    }
}