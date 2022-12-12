package org.taskflow.config.parser;

import org.junit.Test;
import org.taskflow.common.util.gson.GsonUtil;
import org.taskflow.config.entity.OpConfigEntity;
import org.taskflow.config.op.OpConfig;
import org.taskflow.config.op.ParamParserHelper;
import org.taskflow.config.op.param.JsonPathConfig;
import org.taskflow.config.op.param.OpParamConfig;

import java.util.List;
import java.util.Map;

/**
 * Created by ytyht226 on 2022/11/21.
 */
public class ParamParserTest {

    @Test
    public void test() {
        OpConfig opConfig = OpConfigEntity.getOpConfig();
        String str = GsonUtil.toJson(opConfig);
        System.out.println(str);

        OpParamConfig opParamConfig = ParamParserHelper.parse(str, "$.opParamConfig", OpParamConfig.class);
        String proxyObjName = ParamParserHelper.parse(str, "$.opParamConfig.proxyObjName", String.class);
        List<JsonPathConfig> jsonPathConfigList = ParamParserHelper.parse(str, "$.opParamConfig.jsonPathList", List.class);
        Map extMap = ParamParserHelper.parse(str, "$.extMap", Map.class);
        Double score = ParamParserHelper.parse(str, "$.extMap.score", Double.class);
        String address = ParamParserHelper.parse(str, "$.extMap.info.address", String.class);

        System.out.println("opParamConfig:  " + GsonUtil.prettyPrint(opParamConfig));
        System.out.println("proxyObjName:   " + proxyObjName);
        System.out.println("jsonPathConfigList: " + GsonUtil.toJson(jsonPathConfigList));
        System.out.println("extMap: " + GsonUtil.toJson(extMap));
        System.out.println("score:  " + score);
        System.out.println("address:  " + address);
    }

}