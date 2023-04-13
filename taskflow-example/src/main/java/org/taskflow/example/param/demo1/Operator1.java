package org.taskflow.example.param.demo1;


import org.taskflow.core.operator.IOperator;
import org.taskflow.config.op.OpConfig;

/**
 * Created by ytyht226 on 2022/6/23.
 */
public class Operator1 implements IOperator<OpConfig, String> {
    @Override
    public String execute(OpConfig param) throws Exception {
        //业务逻辑部分
        String proxyObjName = param.getOpParamConfig().getProxyObjName();
        System.out.println("1: " + proxyObjName);
        return proxyObjName;
    }
}