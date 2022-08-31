package org.taskflow.example.simpledemo;

import org.taskflow.core.operator.IOperator;

import java.util.concurrent.TimeUnit;

/**
 * Created by ytyht226 on 2022/6/23.
 */
public class Operator1 implements IOperator<Integer, Integer> {
    @Override
    public Integer execute(Integer param) throws Exception {
        //业务逻辑部分
        TimeUnit.SECONDS.sleep(1);
        System.out.println("Operator1...");
        return null;
    }
}