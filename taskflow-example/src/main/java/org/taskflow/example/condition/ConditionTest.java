package org.taskflow.example.condition;

import org.junit.Test;
import org.taskflow.core.DagContextHolder;
import org.taskflow.core.DagEngine;
import org.taskflow.core.callback.ICondition;
import org.taskflow.core.enums.ResultState;
import org.taskflow.core.operator.OperatorResult;
import org.taskflow.core.thread.pool.CustomThreadPool;
import org.taskflow.core.wrapper.OperatorWrapper;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * 准入条件判断
 * Created by ytyht226 on 2022/3/17.
 */
@SuppressWarnings("all")
public class ConditionTest {
    ExecutorService executor = CustomThreadPool.newFixedThreadPoolWrapper(5);
    Operator1 operator1 = new Operator1();
    Operator2 operator2 = new Operator2();
    Operator3 operator3 = new Operator3();
    Operator4 operator4 = new Operator4();
    Operator5 operator5 = new Operator5();

    @Test
    public void test100() {
        for (int i = 0; i < 100; i++) {
            test();
        }
    }

    @Test
    public void test() {
        DagEngine engine = new DagEngine(executor);
        int param = 1;
        OperatorWrapper<Integer, Integer> wrapper1 = new OperatorWrapper<Integer, Integer>()
                .id("1")
                .engine(engine)
                .operator(operator1)
                .context(param)
                ;
        OperatorWrapper<Integer, Integer> wrapper2 = new OperatorWrapper<Integer, Integer>()
                .id("2")
                .engine(engine)
                .operator(operator2)
                .addParamFromWrapperId("1")
                ;
        OperatorWrapper<Integer, Integer> wrapper3 = new OperatorWrapper<Integer, Integer>()
                .id("3")
                .engine(engine)
                .operator(operator3)
                .addParamFromWrapperId("1")
                ;
        OperatorWrapper<Integer, Integer> wrapper4 = new OperatorWrapper<Integer, Integer>()
                .id("4")
                .engine(engine)
                .operator(operator4)
                .addParamFromWrapperId("1")
                ;
        OperatorWrapper<List<Integer>, Integer> wrapper5 = new OperatorWrapper<List<Integer>, Integer>()
                .id("5")
                .engine(engine)
                .operator(operator5)
                .addParamFromWrapperId("2", "3", "4")
                .depend("2", false)
                .depend("3", false)
                .depend("4", false)
                .condition(new Wrapper5Condition())
                ;

        long begin = System.currentTimeMillis();
        engine.runAndWait(900_000);
        long end = System.currentTimeMillis();
        System.out.println("result: " + wrapper5.getOperatorResult().getResult() + ", cost: " + (end - begin));
        if (engine.getEx() != null) {
            engine.getEx().printStackTrace();
        }
    }

    /**
     * 节点5根据 2、3、4的执行情况判断是否执行该节点
     */
    private static class Wrapper5Condition implements ICondition {

        @Override
        public boolean judge(OperatorWrapper wrapper) {
            OperatorResult<Integer> wrapper2Result = DagContextHolder.getOperatorResult("2");
            OperatorResult<Integer> wrapper3Result = DagContextHolder.getOperatorResult("3");
            OperatorResult<Integer> wrapper4Result = DagContextHolder.getOperatorResult("4");
            int result = 0;
            if (wrapper2Result != null && wrapper2Result.getResultState() == ResultState.SUCCESS) {
                result = result + wrapper2Result.getResult();
            }
            if (wrapper3Result != null && wrapper3Result.getResultState() == ResultState.SUCCESS) {
                result = result + wrapper3Result.getResult();
            }
            if (wrapper4Result != null && wrapper4Result.getResultState() == ResultState.SUCCESS) {
                result = result + wrapper4Result.getResult();
            }
            System.out.println("Current result: " + result);
            //如果result >= 5，则返回 true，可以直接执行节点5，因为节点2、4都是sleep(10)，节点3 sleep(100)
            //result(2)+result(4)=6 > 5，执行完节点2、4后就可以执行节点5，此时节点3还在执行中会被中断，最终的
            //执行路径为 1 -> (2,4) -> 5
            return result >= 5;
        }
    }
}