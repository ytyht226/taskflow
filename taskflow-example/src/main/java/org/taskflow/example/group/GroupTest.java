package org.taskflow.example.group;

import org.junit.Test;
import org.taskflow.core.DagEngine;
import org.taskflow.core.wrapper.OperatorWrapper;
import org.taskflow.core.wrapper.OperatorWrapperGroup;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 节点组
 * Created by ytyht226 on 2022/6/23.
 */
@SuppressWarnings("all")
public class GroupTest {
    Operator1 operator1 = new Operator1();
    Operator2 operator2 = new Operator2();
    Operator3 operator3 = new Operator3();
    Operator4 operator4 = new Operator4();
    Operator5 operator5 = new Operator5();
    Operator6 operator6 = new Operator6();
    Operator7 operator7 = new Operator7();
    Operator8 operator8 = new Operator8();
    Operator9 operator9 = new Operator9();
    Operator10 operator10 = new Operator10();
    ExecutorService executor = Executors.newFixedThreadPool(5);

    @Test
    public void test() {
        DagEngine engine = new DagEngine(executor);
        //节点1、2、3、4是一个节点组
        OperatorWrapperGroup group1 = buildGroup1(engine);
        //节点5、6、7、8是一个节点组
        OperatorWrapperGroup group2 = buildGroup2(engine);
        //节点组1的后继节点是节点组2的开始节点
        group1.next(group2.getGroupBeginId());
        //节点组2(结束节点)的后继节点是节点9
        group2.next("9");

        OperatorWrapper<Integer, Integer> wrapper9 = new OperatorWrapper<Integer, Integer>()
                .id("9")
                .engine(engine)
                .operator(operator9)
                //节点9的后继节点是节点10
                .next("10")
                ;
        OperatorWrapper<Integer, Integer> wrapper10 = new OperatorWrapper<Integer, Integer>()
                .id("10")
                .engine(engine)
                .operator(operator10)
                ;
        engine.runAndWait(3000);
    }

    private OperatorWrapperGroup buildGroup1(DagEngine engine) {
        OperatorWrapper<Integer, Integer> wrapper1 = new OperatorWrapper<Integer, Integer>()
                .id("1")
                .engine(engine)
                .operator(operator1)
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

        return new OperatorWrapperGroup(engine)
                .beginWrapperIds("1")
                .endWrapperIds("2", "3", "4")
                .init()
                ;
    }

    private OperatorWrapperGroup buildGroup2(DagEngine engine) {
        OperatorWrapper<Integer, Integer> wrapper5 = new OperatorWrapper<Integer, Integer>()
                .id("5")
                .engine(engine)
                .operator(operator5)
                ;
        OperatorWrapper<Integer, Integer> wrapper6 = new OperatorWrapper<Integer, Integer>()
                .id("6")
                .engine(engine)
                .operator(operator6)
                .depend("5")
                ;
        OperatorWrapper<Integer, Integer> wrapper7 = new OperatorWrapper<Integer, Integer>()
                .id("7")
                .engine(engine)
                .operator(operator7)
                .depend("5")
                ;
        OperatorWrapper<Integer, Integer> wrapper8 = new OperatorWrapper<Integer, Integer>()
                .id("8")
                .engine(engine)
                .operator(operator8)
                .depend("5")
                ;

        return new OperatorWrapperGroup(engine)
                .beginWrapperIds("5")
                .endWrapperIds("6", "7", "8")
                .init()
                ;
    }
}