package org.taskflow.example.param.demo1;


import org.taskflow.core.operator.IOperator;

/**
 * Created by ytyht226 on 2022/6/23.
 */
public class Operator2 implements IOperator<String, Object> {
    @Override
    public Object execute(String param) throws Exception {
        //业务逻辑部分

        Object obj = Class.forName(param).getDeclaredConstructor().newInstance();
        System.out.println("2: " + obj);
        return obj;
    }
}