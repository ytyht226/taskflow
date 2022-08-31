package org.taskflow.example.condition;

import org.taskflow.core.operator.IOperator;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by ytyht226 on 2022/4/1.
 */
public class Operator5 implements IOperator<List<Integer>, Integer> {
    @Override
    public Integer execute(List<Integer> param) throws Exception {
        TimeUnit.MILLISECONDS.sleep(10);
        int sum = 0;
        for (Integer i : param) {
            if (i != null) {
                sum += i;
            }
        }
        return sum;
    }
}