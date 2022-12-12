package org.taskflow.config.op.param;

import lombok.Data;

import java.util.List;

/**
 * OP参数配置
 * Created by ytyht226 on 2022/11/22.
 */
@Data
public class OpParamConfig {
    /**
     * 要执行的目标对象
     */
    private String proxyObjName;
    /**
     * 要执行的目标方法
     */
    private String methodName;
    /**
     * 入参json-path列表，顺序和类型必须与方法定义一致
     */
    private List<JsonPathConfig> jsonPathList;
}