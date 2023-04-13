package org.taskflow.example.param.demo3;


import org.taskflow.core.operator.IOperator;
import org.taskflow.config.op.OpConfig;

import java.util.Map;

/**
 * Created by ytyht226 on 2022/6/23.
 */
public class Operator3 implements IOperator<OpConfig, Map<String, Object>> {
    @Override
    public Map<String, Object> execute(OpConfig context) throws Exception {
        //业务逻辑部分
        Map<String, Object> extMap = context.getExtMap();
        return extMap;
    }
}