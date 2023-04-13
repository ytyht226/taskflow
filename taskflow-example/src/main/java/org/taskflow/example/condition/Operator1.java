package org.taskflow.example.condition;


import org.taskflow.core.operator.IOperator;

import java.util.concurrent.TimeUnit;

/**
 * Created by ytyht226 on 2022/4/1.
 */
public class Operator1 implements IOperator<Integer, Integer> {
    @Override
    public Integer execute(Integer param) throws Exception {
        TimeUnit.MILLISECONDS.sleep(10);
        return param;
    }
}