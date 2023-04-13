package org.taskflow.example.context;


import org.taskflow.core.DagContextHolder;
import org.taskflow.core.operator.IOperator;

/**
 * Created by ytyht226 on 2022/6/23.
 */
public class Operator2 implements IOperator<Void, Integer> {
    @Override
    public Integer execute(Void param) throws Exception {
        //业务逻辑部分
        System.out.println(DagContextHolder.getOperatorResult("1"));
        System.out.println("2...");
        return 2;
    }
}