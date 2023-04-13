package org.taskflow.example.dependencytype;


import org.taskflow.core.operator.IOperator;

import java.util.concurrent.TimeUnit;

/**
 * Created by ytyht226 on 2022/6/23.
 */
public class Operator3 implements IOperator<Integer, Integer> {
    @Override
    public Integer execute(Integer param) throws Exception {
        //业务逻辑部分
        TimeUnit.MILLISECONDS.sleep(300);
        System.out.println("3...");
        return null;
    }
}