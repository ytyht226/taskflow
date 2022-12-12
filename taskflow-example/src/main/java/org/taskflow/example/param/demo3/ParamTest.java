package org.taskflow.example.param.demo3;

import org.junit.Test;
import org.taskflow.common.util.gson.GsonUtil;
import org.taskflow.config.op.OpConfig;
import org.taskflow.config.op.param.OpParamConfig;
import org.taskflow.core.DagEngine;
import org.taskflow.core.wrapper.OperatorWrapper;
import org.taskflow.example.param.entity.OpConfigEntity;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 节点参数来源
 * 使用json-path指定参数来源
 * 执行顺序：1 -> (2、3) -> 4
 * Created by ytyht226 on 2022/6/23.
 */
@SuppressWarnings("all")
public class ParamTest {
    Operator1 operator1 = new Operator1();
    Operator2 operator2 = new Operator2();
    Operator3 operator3 = new Operator3();
    ParamOperator4 operator4 = new ParamOperator4();
    ExecutorService executor = Executors.newFixedThreadPool(5);

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
                .next("2", "3")
                ;
        OperatorWrapper wrapper2 = new OperatorWrapper<OpConfig, OpParamConfig>()
                .id("2")
                .engine(engine)
                .operator(operator2)
                .next("4")
                ;
        OperatorWrapper wrapper3 = new OperatorWrapper<OpConfig, Map<String, Object>>()
                .id("3")
                .engine(engine)
                .operator(operator3)
                .next("4")
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
                          "path":"$2",
                          "type":"org.taskflow.config.op.param.OpParamConfig"
                      },
                      {
                          "path":"$3.info",
                          "type":"java.util.Map"
                      },
                      {
                          "type":"org.taskflow.config.op.param.JsonPathConfig",
                          "value":{
                              "path":"xxx",
                              "type":"java.lang.String"
                          }
                      }
                  ]
              }
          }
         */
        String op4Config = "{\n" +
                "    \"opParamConfig\":{\n" +
                "        \"methodName\":\"apply\",\n" +
                "        \"jsonPathList\":[\n" +
                "            {\n" +
                "                \"path\":\"#.opParamConfig.proxyObjName\",\n" +
                "                \"type\":\"java.lang.String\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"path\":\"$2\",\n" +
                "                \"type\":\"org.taskflow.config.op.param.OpParamConfig\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"path\":\"$3.info\",\n" +
                "                \"type\":\"java.util.Map\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"type\":\"org.taskflow.config.op.param.JsonPathConfig\",\n" +
                "                \"value\":{\n" +
                "                    \"path\":\"xxx\",\n" +
                "                    \"type\":\"java.lang.String\"\n" +
                "                }\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";

        OperatorWrapper wrapper4 = new OperatorWrapper<OperatorWrapper, Map<String, Object>>()
                .id("4")
                .engine(engine)
                /**
                 * 设置节点4的相关配置，当前只有参数配置，即：opParamConfig
                 * @see OpParamConfig
                 */
                .opConfig(op4Config)
                /**
                 * 要执行的目标对象，目标方法通过节点配置指定，即 OpParamConfig 中的 methodName字段
                 */
                .proxyObj(operator4)
                ;

        engine.runAndWait(30000);
        System.out.println(GsonUtil.prettyPrint(wrapper4.getOperatorResult().getResult()));
        if (engine.getEx() != null) {
            engine.getEx().printStackTrace();
        }
    }

}