package org.taskflow.example.choose.op.nest_group;


import org.taskflow.core.operator.IOperator;

import java.util.concurrent.TimeUnit;

/**
 * Created by ytyht226 on 2022/6/23.
 */
public class Operator4 implements IOperator<Integer, Integer> {
    @Override
    public Integer execute(Integer param) throws Exception {
        //业务逻辑部分
        TimeUnit.MILLISECONDS.sleep(1000);
        System.out.println("4...");
        return null;
    }
}