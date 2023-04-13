>以下所有例子的代码示例请参考 [taskflow-example](../taskflow-example)

### 1. 使用请求上下文
详细代码示例：org.taskflow.example.param.demo
![vfdGnA.png](https://s1.ax1x.com/2022/12/12/z4yPsI.png)

所有节点的参数来源都是请求上下文(context)，在初始化引擎时指定请求上下文，可以不用指定每个节点的入参，默认情况下如果没有指定节点入参，引擎在执行节点时会将请求上下文(如果有)当做节点入参
#### 代码示例
```
public void test() {
        //请求上下文
        OpConfig opConfig = OpConfigEntity.getOpConfig();
        //初始化引擎时指定请求上下文
        DagEngine engine = new DagEngine(opConfig, executor);
        OperatorWrapper wrapper1 = new OperatorWrapper<OpConfig, OpConfig>()
                .id("1")
                .engine(engine)
                .operator(operator1)
                .next("2")
                ;
        OperatorWrapper wrapper2 = new OperatorWrapper<OpConfig, OpConfig>()
                .id("2")
                .engine(engine)
                .operator(operator2)
                .next("3")
                ;
        OperatorWrapper wrapper3 = new OperatorWrapper<OpConfig, OpConfig>()
                .id("3")
                .engine(engine)
                .operator(operator3)
                ;

        engine.runAndWait(3000);
        
    }
```

### 2. 使用请求上下文或其它节点结果
详细代码示例：org.taskflow.example.param.demo1
![vfdGnA.png](https://s1.ax1x.com/2022/12/12/z4y1e0.png)

节点1参数是请求上下文，节点2的参数是节点1的返回结果，节点3的参数是节点2的返回结果
#### 代码示例
```
/**
     * 模拟一个根据类名初始化实例的过程
     * 1、节点1的入参是请求上下文，返回结果是上下文中的类名
     * 2、节点2的入参是节点1的结果，返回结果是类的实例(没有设置类的属性)
     * 3、节点3的入参是节点2的结果，返回结果是初始化后的实例(设置完类的属性)
     */
    public void test() {
        //请求上下文
        OpConfig opConfig = OpConfigEntity.getOpConfig();
        DagEngine engine = new DagEngine(executor);
        OperatorWrapper wrapper1 = new OperatorWrapper<OpConfig, String>()
                .id("1")
                .engine(engine)
                .operator(operator1)
                .next("2")
                .context(opConfig) //参数来源是外部变量（请求上下文）
                ;
        OperatorWrapper wrapper2 = new OperatorWrapper<String, Object>()
                .id("2")
                .engine(engine)
                .operator(operator2)
                .next("3")
                .addParamFromWrapperId("1")    //参数来源是其它节点的结果
                ;
        OperatorWrapper wrapper3 = new OperatorWrapper<Object, OpConfig>()
                .id("3")
                .engine(engine)
                .operator(operator3)
                .addParamFromWrapperId("2")    //参数来源是其它节点的结果
                ;

        engine.runAndWait(3000);
        System.out.println(wrapper3.getOperatorResult().getResult());
    }
```

### 3. 使用请求上下文或多个其它节点结果
详细代码示例：org.taskflow.example.param.demo2
![vfdGnA.png](https://s1.ax1x.com/2022/12/12/z4y8oT.png)

节点1参数是请求上下文，节点2和节点3的参数都是节点1的返回结果，节点4的参数是节点2和节点3的返回结果(和上面例子2的情况类似，不同之处在于节点4的入参是List类型)
#### 代码示例
```
public void test() {
        //请求上下文
        OpConfig opConfig = OpConfigEntity.getOpConfig();
        DagEngine engine = new DagEngine(executor);
        OperatorWrapper wrapper1 = new OperatorWrapper<OpConfig, String>()
                .id("1")
                .engine(engine)
                .operator(operator1)
                .next("2")
                .context(opConfig) //参数来源是外部变量（请求上下文）
                ;
        OperatorWrapper wrapper2 = new OperatorWrapper<String, OpConfig>()
                .id("2")
                .engine(engine)
                .operator(operator2)
                .addParamFromWrapperId("1")    //参数来源是其它节点的结果
                ;
        OperatorWrapper wrapper3 = new OperatorWrapper<String, OpConfig>()
                .id("3")
                .engine(engine)
                .operator(operator3)
                .addParamFromWrapperId("1")    //参数来源是其它节点的结果
                ;

        OperatorWrapper wrapper4 = new OperatorWrapper<List<OpConfig>, List<OpConfig>>()
                .id("4")
                .engine(engine)
                .operator(operator4)
                .addParamFromWrapperId("2", "3")    //参数来源多个节点的结果
                ;

        engine.runAndWait(3000);
        if (engine.getEx() != null) {
            engine.getEx().printStackTrace();
        }
        System.out.println(wrapper4.getOperatorResult().getResult());
    }
```

### 4. 使用json-path指定参数
>taskflow使用的是开源的json-path，关于json-path的语法可参考：[JSONPath使用教程](https://blog.csdn.net/qq_36595013/article/details/109455924)，可以使用此链接做在线json-path语法测试：http://jsonpath.com/

**语法说明：**

    #：从请求上下文取值标志；比如 #.name，从上下文获取name值
    $：从其它节点返回结果取值标志；比如 $1.name，从节点1结果中获取name值

**节点参数配置说明：**

    methodName：节点要执行的目标方法
    jsonPathList：目标方法参数列表，参数个数和类型必须和方法定义保持一致
    path：通过json-path语法指定参数来源，必须以 # 或 $ 开头
    type：参数类型，必须和根据path或value解析到的值类型一致
    value：该参数的默认值，不指定path或根据path解析报错时，将参数赋值为默认值

#### 参数配置说明
```
{
    "opParamConfig":{
        "methodName":"apply",
        "jsonPathList":[
            {
                "path":"#.opParamConfig.proxyObjName",
                "type":"xxx",
                "value":{

                }
            }
        ]
    }
}
```

#### 4.1 将json-path的结果作为入参
详细代码示例：org.taskflow.example.param.demo3
![vfdGnA.png](https://s1.ax1x.com/2022/12/12/z4ytW4.png)

如上图，节点4有4个参数，下面是关于各参数的配置说明：

    参数1：#.opParamConfig.proxyObjName，参数是请求上下文中的字段
    参数2：$2，参数是节点2返回的结果
    参数3：$3.info，参数是节点3返回结果的info字段
    参数4：配置的默认值

##### 节点4参数来源配置
```
{
    "opParamConfig":{
        "methodName":"apply",
        "jsonPathList":[
            {
                "path":"#.opParamConfig.proxyObjName",  //请求上下文中的字段
                "type":"java.lang.String"   //path值的类型，必须与方法参数类型一致
            },
            {
                "path":"$2",    //节点2返回的结果
                "type":"org.taskflow.config.op.param.OpParamConfig"
            },
            {
                "path":"$3.info",   //节点3返回结果的info字段
                "type":"java.util.Map"
            },
            {
                "type":"org.taskflow.config.op.param.JsonPathConfig",
                "value":{   //默认值
                    "path":"xxx",
                    "type":"java.lang.String"
                }
            }
        ]
    }
}
```
##### 代码示例
```
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

        //节点4参数来源配置
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
    }
```

#### 4.2 将json-path的结果作为入参，解析参数子字段
详细代码示例：org.taskflow.example.param.demo4
![vfdGnA.png](https://s1.ax1x.com/2022/12/12/z4ywO1.png)

如上图，节点4有4个参数，下面是关于各参数的配置说明：

    参数1：#.opParamConfig.proxyObjName，参数是请求上下文中的字段
    参数2：$2.opParamConfig，参数是节点2返回的结果；在该例子中，执行节点2时，引擎上下文中只有请求上下文和节点1的结果，没有节点2的结果，根据该path会解析失败，此时会将value作为默认值；如果改成$1.opParamConfig，就可以正常解析节点1的字段值
    参数3：没有配置path，默认使用value作为参数值，同时会解析value中的子字段，如果子字段的值以 $ 或 # 开头，就会根据json-path解析具体值
    参数4：解析逻辑与参数3情况类似，不同之处在于，该参数的value配置中参数来源包含两部分：json-path、默认值(不以$ 或 # 开头)

##### 节点2参数来源配置
```
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
```
##### 代码示例
```
public void test() {
        //参数子字段解析器
        IParamParseOperator paramParseOperator = new RecurseParamParseOperator()
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

        //节点2参数来源配置
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
                .opConfig(op2Config, paramParseOperator)    //需要指定使用参数子字段解析器
                .proxyObj(paramOperator2)
                ;

        engine.runAndWait(300000);
        System.out.println(GsonUtil.prettyPrint(wrapper2.getOperatorResult().getResult()));
        if (engine.getEx() != null) {
            engine.getEx().printStackTrace();
        }
```
