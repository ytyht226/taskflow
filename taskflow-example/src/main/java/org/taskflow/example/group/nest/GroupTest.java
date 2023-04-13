package org.taskflow.example.group.nest;

import org.taskflow.core.DagEngine;
import org.taskflow.core.thread.pool.CustomThreadPool;
import org.taskflow.core.wrapper.OperatorWrapper;
import org.taskflow.core.wrapper.OperatorWrapperGroup;
import org.junit.Test;

import java.util.concurrent.ExecutorService;

/**
 * 嵌套节点组
 * Created by ytyht226 on 2022/6/23.
 */
@SuppressWarnings("all")
public class GroupTest {
    ExecutorService executor = CustomThreadPool.newFixedThreadPoolWrapper(5);
    Operator1 operator1 = new Operator1();
    Operator2 operator2 = new Operator2();
    Operator3 operator3 = new Operator3();
    Operator4 operator4 = new Operator4();
    Operator5 operator5 = new Operator5();
    Operator6 operator6 = new Operator6();
    Operator7 operator7 = new Operator7();
    Operator8 operator8 = new Operator8();
    Operator9 operator9 = new Operator9();

    @Test
    public void test() {
        DagEngine engine = new DagEngine(executor);
        //节点2、3、4、5是节点组 group1
        OperatorWrapperGroup group1 = buildGroup1(engine);
        //节点6、7是节点组 group2
        OperatorWrapperGroup group2 = buildGroup2(engine);
        //节点组 group1、group2 和 节点8是节点组 group3
        OperatorWrapperGroup group3 = buildGroup3(engine, group1, group2);

        //节点1的后继节点是节点组group3
        OperatorWrapper<Integer, Integer> wrapper1 = new OperatorWrapper<Integer, Integer>()
                .id("1")
                .engine(engine)
                .operator(operator1)
                .next(group3.getGroupBeginId())
                ;
        //节点组3的后继节点是节点9
        group3.next("9");

        OperatorWrapper<Integer, Integer> wrapper9 = new OperatorWrapper<Integer, Integer>()
                .id("9")
                .engine(engine)
                .operator(operator9)
                ;
        engine.runAndWait(300_000);
        if (engine.getEx() != null) {
            engine.getEx().printStackTrace();
        }
    }

    private OperatorWrapperGroup buildGroup1(DagEngine engine) {

        OperatorWrapper<Integer, Integer> wrapper2 = new OperatorWrapper<Integer, Integer>()
                .id("2")
                .engine(engine)
                .operator(operator2)
                ;
        OperatorWrapper<Integer, Integer> wrapper3 = new OperatorWrapper<Integer, Integer>()
                .id("3")
                .engine(engine)
                .operator(operator3)
                .depend("2")
                ;
        OperatorWrapper<Integer, Integer> wrapper4 = new OperatorWrapper<Integer, Integer>()
                .id("4")
                .engine(engine)
                .operator(operator4)
                .depend("2")
                ;
        OperatorWrapper<Integer, Integer> wrapper5 = new OperatorWrapper<Integer, Integer>()
                .id("5")
                .engine(engine)
                .operator(operator5)
                .depend("3", "4")
                ;

        return new OperatorWrapperGroup(engine)
                .beginWrapperIds("2")
                .endWrapperIds("5")
                .children("2", "3", "4", "5")
                .init()
                ;
    }

    private OperatorWrapperGroup buildGroup2(DagEngine engine) {
        OperatorWrapper<Integer, Integer> wrapper6 = new OperatorWrapper<Integer, Integer>()
                .id("6")
                .engine(engine)
                .operator(operator6)
                ;
        OperatorWrapper<Integer, Integer> wrapper7 = new OperatorWrapper<Integer, Integer>()
                .id("7")
                .engine(engine)
                .operator(operator7)
                .depend("6")
                ;

        return new OperatorWrapperGroup(engine)
                .beginWrapperIds("6")
                .endWrapperIds("7")
                .children("6", "7")
                .init()
                ;
    }

    private OperatorWrapperGroup buildGroup3(DagEngine engine, OperatorWrapperGroup group1, OperatorWrapperGroup group2) {
        group1.next("8");
        group2.next("8");

        OperatorWrapper<Integer, Integer> wrapper8 = new OperatorWrapper<Integer, Integer>()
                .id("8")
                .engine(engine)
                .operator(operator8)
                ;

        return new OperatorWrapperGroup(engine)
                .beginWrapperIds(group1.getGroupBeginId(), group2.getGroupBeginId())
                .endWrapperIds("8")
                .children(group1.getId(), group2.getId(), "8")
                .init()
                ;
    }
}