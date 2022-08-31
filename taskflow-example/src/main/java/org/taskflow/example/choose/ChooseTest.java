package org.taskflow.example.choose;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.taskflow.core.DagEngine;
import org.taskflow.core.wrapper.OperatorWrapper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 分支选择
 * Created by ytyht226 on 2022/6/23.
 */
@SuppressWarnings("all")
public class ChooseTest {
    Operator1 operator1 = new Operator1();
    Operator2 operator2 = new Operator2();
    Operator3 operator3 = new Operator3();
    Operator4 operator4 = new Operator4();
    ExecutorService executor = Executors.newFixedThreadPool(5);

    @Test
    public void test() {
        DagEngine engine = new DagEngine(executor);
        OperatorWrapper<Integer, Integer> wrapper1 = new OperatorWrapper<Integer, Integer>()
                .id("1")
                .engine(engine)
                .operator(operator1)
                .chooseNext((w) -> {
                    Integer result = (Integer) w.getOperatorResult().getResult();
                    if (result == 2) {
                        return Sets.newHashSet("2");
                    } else if(result == 3) {
                        return Sets.newHashSet("3");
                    } else {
                        return Sets.newHashSet("4");
                    }
                });
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

        engine.runAndWait(300000);
    }
}