package org.taskflow.example.param.demo;


import org.taskflow.core.operator.IOperator;
import org.taskflow.config.op.OpConfig;

/**
 * Created by ytyht226 on 2022/6/23.
 */
public class Operator1 implements IOperator<OpConfig, OpConfig> {
    @Override
    public OpConfig execute(OpConfig param) throws Exception {
        //业务逻辑部分

        param.getExtMap().put("op1", "op1");
        return param;
    }
}