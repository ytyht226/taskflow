package org.taskflow.config.entity;


import org.taskflow.config.op.OpConfig;
import org.taskflow.config.op.param.JsonPathConfig;
import org.taskflow.config.op.param.OpParamConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ytyht226 on 2022/11/23.
 */
public class OpConfigEntity {

    public static OpConfig getOpConfig() {
        OpConfig opConfig = new OpConfig();

        opConfig.setOpParamConfig(getOpParamConfig());
        Map<String, Object> extMap = new ConcurrentHashMap<>();
        extMap.put("city", "bj");
        extMap.put("code", 100);
        extMap.put("score", 100.05);

        Map<String, Object> infoMap = new ConcurrentHashMap<>();
        infoMap.put("address", "hd");
        infoMap.put("age", 18);
        extMap.put("info", infoMap);

        opConfig.setExtMap(extMap);

        return opConfig;
    }

    private static OpParamConfig getOpParamConfig() {
        OpParamConfig opParamConfig = new OpParamConfig();
        opParamConfig.setProxyObjName("org.taskflow.config.entity.OpConfigEntity");
        opParamConfig.setMethodName("test");
        opParamConfig.setJsonPathList(getJsonPathConfigList());
        return opParamConfig;
    }

    private static List<JsonPathConfig> getJsonPathConfigList() {
        List<JsonPathConfig> jsonPathConfigList = new ArrayList<>();

        JsonPathConfig config1 = new JsonPathConfig();
        config1.setPath("#.request.param");
        config1.setType("java.lang.String");
        config1.setValue("test");

        JsonPathConfig config2 = new JsonPathConfig();
        config2.setPath("$1.response.param");
        config2.setType("java.lang.Integer");

        jsonPathConfigList.add(config1);
        jsonPathConfigList.add(config2);

        return jsonPathConfigList;
    }
}