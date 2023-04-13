package org.taskflow.example.choose.op.group_group;

import com.google.common.collect.Sets;
import org.taskflow.core.DagEngine;
import org.taskflow.core.thread.pool.CustomThreadPool;
import org.taskflow.core.wrapper.OperatorWrapper;
import org.taskflow.core.wrapper.OperatorWrapperGroup;
import org.junit.Test;

import java.util.concurrent.ExecutorService;

/**
 * 节点选择
 * 待选择的都是节点组
 * Created by ytyht226 on 2022/6/23.
 */
@SuppressWarnings("all")
public class ChooseOpTest {
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

        //节点2、4是一个节点组
        OperatorWrapperGroup group1 = buildGroup1(engine);
        //节点3、5是一个节点组
        OperatorWrapperGroup group2 = buildGroup2(engine);

        OperatorWrapper<Integer, Integer> wrapper1 = new OperatorWrapper<Integer, Integer>()
                .id("1")
                .engine(engine)
                .operator(operator1)
                .next(group1.getGroupBeginId())
                .next(group2.getGroupBeginId())
                .chooseOp(wrapper -> {
                    //节点组1、2都执行
                    return Sets.newHashSet(group1.getGroupBeginId(), group2.getGroupBeginId());
                })
                ;

        group1.next("6");
        group2.next("6");

        OperatorWrapper<Integer, Integer> wrapper6 = new OperatorWrapper<Integer, Integer>()
                .id("6")
                .engine(engine)
                .operator(operator6)
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
                .next("4")
                ;
        OperatorWrapper<Integer, Integer> wrapper3 = new OperatorWrapper<Integer, Integer>()
                .id("4")
                .engine(engine)
                .operator(operator4)
                ;

        return new OperatorWrapperGroup(engine)
                .beginWrapperIds("2")
                .endWrapperIds("4")
                .children("2", "4")
                .init()
                ;
    }

    private OperatorWrapperGroup buildGroup2(DagEngine engine) {
        OperatorWrapper<Integer, Integer> wrapper3 = new OperatorWrapper<Integer, Integer>()
                .id("3")
                .engine(engine)
                .operator(operator3)
                .next("5")
                ;
        OperatorWrapper<Integer, Integer> wrapper5 = new OperatorWrapper<Integer, Integer>()
                .id("5")
                .engine(engine)
                .operator(operator5)
                ;

        return new OperatorWrapperGroup(engine)
                .beginWrapperIds("3")
                .endWrapperIds("5")
                .children("3", "5")
                .init()
                ;
    }
}