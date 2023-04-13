package org.taskflow.example.param.demo1;


import org.taskflow.core.operator.IOperator;
import org.taskflow.config.op.OpConfig;
import org.taskflow.config.op.param.OpParamConfig;

/**
 * Created by ytyht226 on 2022/6/23.
 */
public class Operator3 implements IOperator<Object, OpConfig> {
    @Override
    public OpConfig execute(Object param) throws Exception {
        //业务逻辑部分
        OpConfig opConfig = (OpConfig) param;
        OpParamConfig opParamConfig = new OpParamConfig();
        opParamConfig.setProxyObjName("Operator3");
        opConfig.setOpParamConfig(opParamConfig);
        return opConfig;
    }
}