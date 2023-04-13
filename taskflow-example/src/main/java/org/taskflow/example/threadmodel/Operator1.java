package org.taskflow.example.threadmodel;


import org.taskflow.core.operator.IOperator;

import java.util.concurrent.TimeUnit;

/**
 * Created by ytyht226 on 2022/6/23.
 */
public class Operator1 implements IOperator<Void, Integer> {

    @Override
    public Integer execute(Void param) throws Exception {
        TimeUnit.SECONDS.sleep(3);
        System.out.println("1...");
        return 1;
    }
}