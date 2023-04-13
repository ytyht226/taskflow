package org.taskflow.example.param.demo4;

import org.junit.Test;
import org.taskflow.common.util.gson.GsonUtil;
import org.taskflow.config.op.OpConfig;
import org.taskflow.core.DagEngine;
import org.taskflow.core.operator.IParamParseOperator;
import org.taskflow.core.operator.RecurseParamParseOperator;
import org.taskflow.core.thread.pool.CustomThreadPool;
import org.taskflow.core.wrapper.OperatorWrapper;
import org.taskflow.example.param.entity.OpConfigEntity;

import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * 节点参数来源
 * 使用json-path指定参数来源
 * 执行顺序：1 -> (2、3) -> 4 -> 5
 * Created by ytyht226 on 2022/6/23.
 */
@SuppressWarnings("all")
public class ParamTest {
    ExecutorService executor = CustomThreadPool.newFixedThreadPoolWrapper(5);
    Operator1 operator1 = new Operator1();
    ParamOperator2 paramOperator2 = new ParamOperator2();
    IParamParseOperator paramParseOperator = new RecurseParamParseOperator();

    @Test
    public void test() {
        //请求上下文
        OpConfig opConfig = OpConfigEntity.getOpConfig();

        System.out.println(GsonUtil.prettyPrint(opConfig));
        System.out.println("==========================");

        DagEngine engine = new DagEngine(opConfig, executor);
        OperatorWrapper wrapper1 = new OperatorWrapper<OpConfig, OpConfig>()
                .id("1")
                .engine(engine)
                .operator(operator1)
                .next("2")
                ;

        //测试配置示例：
        /**
          {
              "opParamConfig":{
                  "methodName":"apply",
                  "jsonPathList":[
                      {
                          "path":"#.opParamConfig.proxyObjName",
                          "type":"java.lang.String"
                      },
                      {
                          "path":"$2.opParamConfig",
                          "type":"org.taskflow.config.op.param.OpParamConfig",
                          "value":{
                              "proxyObjName":"xxx",
                              "jsonPathList":"$1.opParamConfig.jsonPathList[:1]"
                          }
                      },
                      {
                          "type":"org.taskflow.config.op.param.JsonPathConfig",
                          "value":{
                              "path":"$1.opParamConfig.proxyObjName",
                              "type":"java.lang.String"
                          }
                      },
                      {
                          "type":"java.util.Map",
                          "value":{
                              "address":"$1.extMap.info.address",
                              "age":"$1.extMap.info.age",
                              "proxyObjName":"$1.opParamConfig.proxyObjName",
                              "methodName":"test"
                          }
                      }
                  ]
              }
          }
         */
        String op2Config = "{\n" +
                "    \"opParamConfig\":{\n" +
                "        \"methodName\":\"apply\",\n" +
                "        \"jsonPathList\":[\n" +
                "            {\n" +
                "                \"path\":\"#.opParamConfig.proxyObjName\",\n" +
                "                \"type\":\"java.lang.String\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"path\":\"$2.opParamConfig\",\n" +
                "                \"type\":\"org.taskflow.config.op.param.OpParamConfig\",\n" +
                "                \"value\":{\n" +
                "                    \"proxyObjName\":\"xxx\",\n" +
                "                    \"jsonPathList\":\"$1.opParamConfig.jsonPathList[:1]\"\n" +
                "                }\n" +
                "            },\n" +
                "            {\n" +
                "                \"type\":\"org.taskflow.config.op.param.JsonPathConfig\",\n" +
                "                \"value\":{\n" +
                "                    \"path\":\"$1.opParamConfig.proxyObjName\",\n" +
                "                    \"type\":\"java.lang.String\"\n" +
                "                }\n" +
                "            },\n" +
                "            {\n" +
                "                \"type\":\"java.util.Map\",\n" +
                "                \"value\":{\n" +
                "                    \"address\":\"$1.extMap.info.address\",\n" +
                "                    \"age\":\"$1.extMap.info.age\",\n" +
                "                    \"proxyObjName\":\"$1.opParamConfig.proxyObjName\",\n" +
                "                    \"methodName\":\"test\"\n" +
                "                }\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";

        OperatorWrapper wrapper2 = new OperatorWrapper<OperatorWrapper, Map<String, Object>>()
                .id("2")
                .engine(engine)
                .opConfig(op2Config, paramParseOperator)
                .proxyObj(paramOperator2)
                ;

        engine.runAndWait(300_000);
        System.out.println(GsonUtil.prettyPrint(wrapper2.getOperatorResult().getResult()));
        if (engine.getEx() != null) {
            engine.getEx().printStackTrace();
        }
    }

}