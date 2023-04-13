package org.taskflow.example.listener;


import org.taskflow.core.operator.IOperator;

import java.util.concurrent.TimeUnit;

/**
 * Created by ytyht226 on 2022/4/1.
 */
public class Operator3 implements IOperator<Integer, Integer> {
    @Override
    public Integer execute(Integer param) throws Exception {
        TimeUnit.MILLISECONDS.sleep(100);
        return param * 3;
    }
}