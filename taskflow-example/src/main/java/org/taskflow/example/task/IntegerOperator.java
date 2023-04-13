package org.taskflow.example.task;

import org.taskflow.core.operator.IOperator;

import java.util.concurrent.TimeUnit;

/**
 * Created by ytyht226 on 2023/3/16.
 */
public class IntegerOperator implements IOperator<Integer, Integer> {
    @Override
    public Integer execute(Integer param) throws Exception {
        TimeUnit.SECONDS.sleep(1);
        return 10 * param;
    }
}