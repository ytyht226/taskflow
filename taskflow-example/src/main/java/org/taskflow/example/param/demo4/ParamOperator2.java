package org.taskflow.example.param.demo4;


import org.taskflow.config.op.param.JsonPathConfig;
import org.taskflow.config.op.param.OpParamConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ytyht226 on 2022/11/22.
 */
public class ParamOperator2 {

    public Map<String, Object> apply(String proxyObjName, OpParamConfig opParamConfig, JsonPathConfig jsonPathConfig, Map<String, Object> info) {
        Map<String, Object> map = new HashMap<>();
        map.put("proxyObjName", proxyObjName);
        map.put("opParamConfig", opParamConfig);
        map.put("jsonPathConfig", jsonPathConfig);
        map.put("info", info);
        return map;
    }

}