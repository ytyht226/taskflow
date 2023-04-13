package org.taskflow.example.param.demo3;

import org.taskflow.config.op.param.JsonPathConfig;
import org.taskflow.config.op.param.OpParamConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ytyht226 on 2022/11/22.
 */
public class ParamOperator4 {

    public Map<String, Object> apply(String proxyObjName, OpParamConfig opParamConfig, Map<String, Object> info, JsonPathConfig pathConfig) {
        Map<String, Object> map = new HashMap<>();
        map.put("proxyObjName", proxyObjName);
        map.put("opParamConfig", opParamConfig);
        map.put("info", info);
        map.put("pathConfig", pathConfig);
        return map;
    }

}