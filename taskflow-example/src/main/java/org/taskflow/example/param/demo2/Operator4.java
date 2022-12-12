package org.taskflow.example.param.demo2;



import org.taskflow.config.op.OpConfig;
import org.taskflow.config.op.param.OpParamConfig;
import org.taskflow.core.operator.IOperator;

import java.util.List;

/**
 * Created by ytyht226 on 2022/6/23.
 */
public class Operator4 implements IOperator<List<OpConfig>, List<OpConfig>> {
    @Override
    public List<OpConfig> execute(List<OpConfig> param) throws Exception {
        for (OpConfig opConfig : param) {
            OpParamConfig opParamConfig = opConfig.getOpParamConfig();
            if (opParamConfig.getProxyObjName().equals("Operator2")) {
                opParamConfig.setMethodName("Operator2_method");
            } else if (opParamConfig.getProxyObjName().equals("Operator3")) {
                opParamConfig.setMethodName("Operator3_method");
            }
        }
        return param;
    }
}