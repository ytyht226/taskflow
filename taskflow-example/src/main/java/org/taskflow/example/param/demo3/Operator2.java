package org.taskflow.example.param.demo3;


import org.taskflow.config.op.OpConfig;
import org.taskflow.config.op.param.OpParamConfig;
import org.taskflow.core.operator.IOperator;

/**
 * Created by ytyht226 on 2022/6/23.
 */
public class Operator2 implements IOperator<OpConfig, OpParamConfig> {
    @Override
    public OpParamConfig execute(OpConfig context) throws Exception {
        //业务逻辑部分

        OpParamConfig opParamConfig = context.getOpParamConfig();
        return opParamConfig;
    }
}