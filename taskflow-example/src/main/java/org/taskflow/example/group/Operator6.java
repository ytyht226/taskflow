package org.taskflow.example.group;


import org.taskflow.core.operator.IOperator;

/**
 * Created by ytyht226 on 2022/6/23.
 */
public class Operator6 implements IOperator<Integer, Integer> {
    @Override
    public Integer execute(Integer param) throws Exception {
        //业务逻辑部分
        System.out.println("6...");
        return null;
    }
}