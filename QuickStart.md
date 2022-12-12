#### 一、新建编排流程
>**假如要实现一个如下的编排流程，1、2并行执行完再执行3**
>
>![vfdGnA.png](https://s1.ax1x.com/2022/09/06/vHACPs.png)
##### 1. 实现IOperator接口，开发业务逻辑
```
//IOperator接口定义
@FunctionalInterface
public interface IOperator<P, V> {
  	
    //业务要实现的逻辑部分
    V execute(P param) throws Exception;
  	
    //自定义OP默认返回值，比如节点执行异常时
    default V defaultValue() {
        return null;
    }
}

//节点1定义
public class Operator1 implements IOperator<Integer, Integer> {
    @Override
    public Integer execute(Integer param) throws Exception {
        /**
         业务逻辑部分
         */
        return null;
    }
}
//节点2定义
public class Operator2 implements IOperator<Integer, Integer> {
    @Override
    public Integer execute(Integer param) throws Exception {
        /**
         业务逻辑部分
         */
        return null;
    }
}
//节点3定义
public class Operator3 implements IOperator<Integer, Integer> {
    @Override
    public Integer execute(Integer param) throws Exception {
        /**
         业务逻辑部分
         */
        return null;
    }
}
```
**初始化Operator:**

    Operator1 operator1 = new Operator1();
    Operator2 operator2 = new Operator2();
    Operator3 operator3 = new Operator3();

##### 2. 初始化DAG执行引擎
```
ExecutorService executor = Executors.newFixedThreadPool(5);	//业务根据实际情况使用合适的线程池
DagEngine engine = new DagEngine(executor);
```
##### 3. 定义包装类，指定依赖关系
```
OperatorWrapper<Integer, Integer> wrapper1 = new OperatorWrapper<Integer, Integer>()
                .id("1")
                .engine(engine)
                .operator(operator1)
                ;
OperatorWrapper<Integer, Integer> wrapper2 = new OperatorWrapper<Integer, Integer>()
                .id("2")
                .engine(engine)
                .operator(operator2)
                ;
OperatorWrapper<Integer, Integer> wrapper3 = new OperatorWrapper<Integer, Integer>()
                .id("3")
                .engine(engine)
                .operator(operator3)
                .depend("1", "2")
                ;
```
##### 4. 启动DAG引擎
```
engine.runAndWait(3000);	//设置编排流程执行的超时时间
```
##### 完整代码示例
```
public class DemoTest {
    Operator1 operator1 = new Operator1();
    Operator2 operator2 = new Operator2();
    Operator3 operator3 = new Operator3();
    ExecutorService executor = Executors.newFixedThreadPool(5);

    @Test
    public void test() {
        DagEngine engine = new DagEngine(executor);
        OperatorWrapper<Integer, Integer> wrapper1 = new OperatorWrapper<Integer, Integer>()
                .id("1")
                .engine(engine)
                .operator(operator1)
                ;
        OperatorWrapper<Integer, Integer> wrapper2 = new OperatorWrapper<Integer, Integer>()
                .id("2")
                .engine(engine)
                .operator(operator2)
                ;
        OperatorWrapper<Integer, Integer> wrapper3 = new OperatorWrapper<Integer, Integer>()
                .id("3")
                .engine(engine)
                .operator(operator3)
                .depend("1", "2")
                ;
        engine.runAndWait(3000);
    }
}
```
#### 二、其它使用说明

**以下示例，节点之间的连线如果是实线代表是强依赖关系，如果是虚线代表是弱依赖关系**

##### 1. 指定线程池

初始化DAG执行引擎时，可以根据不同的业务使用不同的线程池，以达到业务隔离的效果，不显示声明时，使用框架默认的线程池
```
ExecutorService executor = Executors.newFixedThreadPool(5);
DagEngine engine = new DagEngine(executor);
```
##### 2. 设置超时时间

启动DAG引擎时，需要设置编排流程执行的超时时间，执行时间达到超时阈值后，未开始执行的节点不再执行，执行中的节点会被中断
```
engine.runAndWait(300, "1", "2");//超时时间单位：毫秒
```
##### 3. 节点包装类

节点(OP)是实现某一特定功能的组件，在一个编排流程中，不同OP之间的依赖关系需要通过包装类(wrapper)来指定，创建包装类时指定目标OP并设置唯一的id
```
OperatorWrapper<Integer, Integer> wrapper1 = new OperatorWrapper<Integer, Integer>()
                .id("a")//需要保证在整个编排流程中id唯一
                .operator(operator1)//目标OP
```
##### 4. 节点依赖

在创建wrapper时，可以通过如下几种方式指定OP之间的依赖关系
```
next(String ... wrapperIds)//当前节点后续的节点
depend(String ... wrapperIds)//当前节点依赖的其它节点
addParamFromWrapperId(String ... fromWrapperIds)//当前节点OP的入参是其它节点的返回值(说明依赖其它节点)
```
##### 5. OP入参来源

具体使用方法见：[指定参数来源](./ParamSource.md)

##### 6. 全局上下文

DAG引擎执行过程中，每个OP节点的计算结果都会保存到上下文(DagContext)中，使用上下文获取OP结果的方式如下:
```
DagContextHolder.getOperatorResult(id)//id表示的是具体的OP
```
##### 7. DAG引擎回调接口
```
before(IDagCallback callback);//DAG引擎执行前回调
after(IDagCallback callback);//DAG引擎执行后回调
```
##### 8. OP回调接口
```
beforeOp(ICallable callback);//每个OP执行前的回调
afterOp(ICallable callback);//每个OP执行后的回调
```
##### 9. 线程模型

![vfdGnA.png](https://s1.ax1x.com/2022/09/06/vHmp3d.png) ![vfdGnA.png](https://s1.ax1x.com/2022/09/07/vHmJ5F.png)

如上图有两种线程模型：阻塞模式、非阻塞模式
阻塞模式：主线程等待编排流程的执行结束（正常结束、超时、异常等）
非阻塞模式：主线程调用编排流程后立即返回，继续执行后续逻辑，编排流程执行结束后，通过提供的回调接口进行通知
默认情况是阻塞模式；如果初始化DAG引擎时指定了回调接口(dagCallback)，则使用非阻塞模式
```
engine.runAndWait(9000, "1");   //阻塞模式
engine.runWithCallback(9000, dagCallback, "1"); //非阻塞模式，dagCallback是一个回调接口
```
##### 10. 依赖关系类型

![vfdGnA.png](https://s1.ax1x.com/2022/09/06/vHmCjI.png)

   * 强依赖，节点之间默认的依赖关系，只有前面的节点执行结束才可以执行后续的节点，编排流程的执行时间取决于所有节点中执行时间最长的
   * 弱依赖，不同于强依赖的执行逻辑，只要节点依赖的其它节点中有一个执行结束就可以执行当前节点，编排流程的执行时间取决于所有节点中执行时间最短的

如上图，4强依赖2、3节点，弱依赖a节点，假如节点1在某次执行时需要使用500ms，2、3使用300ms，则2、3执行完之后就可以直接执行节点4，执行节点4时，DAG引擎会中断节点1的执行，释放线程资源；更进一步，如果4全部弱依赖与1、2、3节点，则1、2、3节点中只要有一个执行完就可以执行4，其它节点会被中断

可以使用如下方式指定弱依赖:

```
next(String wrapperId, boolean selfIsMust);//当前节点的后续节点，selfIsMust：true（强依赖），false（弱依赖）
depend(String wrapperId, boolean isMust); //当前节点依赖的其它节点，isMust：true（强依赖），false（弱依赖）
```
##### 11. 准入条件判断

![vfdGnA.png](https://s1.ax1x.com/2022/09/06/vHmFDP.png)

类似于节点的准入条件；在某些场景下，需要根据依赖节点的执行结果动态的判断是否执行当前节点，比如在推荐的多路召回阶段，召回源可以设置多个，并发召回时如果召回的结果已满足召回策略(比如召回条数不少于100条)，可以不等待其它召回结果的返回而直接执行后续的逻辑；如下图，4 依赖 1、2、3 三个节点，如果节点1、2、3中有任意两个执行完就可以执行节点4，就可以使用条件判断；通过使用条件判断，可以有效的提升编排的执行效率；实现的逻辑是1、2、3中任意一个节点执行完后都执行节点4的前置判断，看是否满足准入条件，如果满足就执行，否则等待其它节点执行完

代码示例：
```
/**
    * 节点4根据 1、2、3执行情况判断是否执行该节点
    */
@SuppressWarnings("all")
private static class Wrapper4Condition implements ICondition {

    @Override
    public boolean call(OperatorWrapper wrapper) {
        OperatorResult<Integer> wrapper1Result = DagContextHolder.getOperatorResult("1");
        OperatorResult<Integer> wrapper2Result = DagContextHolder.getOperatorResult("2");
        OperatorResult<Integer> wrapper3Result = DagContextHolder.getOperatorResult("3");
        int result = 0;
        if (wrapper1Result != null && wrapper1Result.getResultState() == ResultState.SUCCESS) {
            result = result + wrapper1Result.getResult();
        }
        if (wrapper2Result != null && wrapper2Result.getResultState() == ResultState.SUCCESS) {
            result = result + wrapper2Result.getResult();
        }
        if (wrapper3Result != null && wrapper3Result.getResultState() == ResultState.SUCCESS) {
            result = result + wrapper3Result.getResult();
        }
        return result >= 5;
    }
}
```
##### 12. 分支选择

![vfdGnA.png](https://s1.ax1x.com/2022/09/06/vHmVUS.png)

根据节点的计算结果动态的选择要执行的子节点，如上图，默认情况下节点1执行完后会并行执行节点2、3、4；如果想根据节点1的结果，判断后续要执行的节点，可以使用分支选择的功能

代码示例:
```
public void test() {
    DagEngine engine = new DagEngine(executor);
    OperatorWrapper<Integer, Integer> wrapper1 = new OperatorWrapper<Integer, Integer>()
            .id("1")
            .engine(engine)
            .operator(operator1)
            .chooseNext((w) -> {	//分支选择的逻辑，根据节点1的结果判断后续要执行的节点（可以有多个）
                Integer result = (Integer) w.getOperatorResult().getResult();
                if (result == 2) {
                    return Sets.newHashSet("2");
                } else if(result == 3) {
                    return Sets.newHashSet("3");
                } else {
                    return Sets.newHashSet("4");
                }
            });
            ;
    OperatorWrapper<Integer, Integer> wrapper2 = new OperatorWrapper<Integer, Integer>()
            .id("2")
            .engine(engine)
            .operator(operator2)
            .depend("1")
            ;
    OperatorWrapper<Integer, Integer> wrapper3 = new OperatorWrapper<Integer, Integer>()
            .id("3")
            .engine(engine)
            .operator(operator3)
            .depend("1")
            ;
    OperatorWrapper<Integer, Integer> wrapper4 = new OperatorWrapper<Integer, Integer>()
            .id("4")
            .engine(engine)
            .operator(operator4)
            .depend("1")
            ;

    engine.runAndWait(3000, "1");
}
```
##### 13. 节点执行状态监听器

节点运行的结果有如下三种状态：
```
START	//开始
SUCCESS	//正常结束
ERROR	//异常结束
```
可以给节点添加不同执行阶段的监听器，节点执行到特定状态时会触发监听器的逻辑，在监听器逻辑中可以做一下额外的工作，比如日志打印、执行结果的上报等；不同于节点回调接口的执行，监听器的逻辑是在其它线程中执行和当前节点不在一个线程中

代码示例：
```
public void test() {
    OperatorListener listener1 = getListener();
    DagEngine engine = new DagEngine(executor);
    OperatorWrapper<Integer, Integer> wrapper1 = new OperatorWrapper<Integer, Integer>()
            .id("1")
            .engine(engine)
            .operator(operator1)
            .addListener(listener1, OperatorEventEnum.START)	//START状态的监听器
            .addListener(listener1, OperatorEventEnum.ERROR)	//ERROR状态的监听器
            .addListener(listener1, OperatorEventEnum.SUCCESS)	//SUCCESS状态的监听器
            ;
    OperatorWrapper<Integer, Integer> wrapper2 = new OperatorWrapper<Integer, Integer>()
            .id("2")
            .engine(engine)
            .operator(operator2)
            .addParamFromWrapperId("1")
            ;
    OperatorWrapper<Integer, Integer> wrapper3 = new OperatorWrapper<Integer, Integer>()
            .id("3")
            .engine(engine)
            .operator(operator3)
            .addParamFromWrapperId("2")
            ;

    engine.runAndWait(900000, wrapper1);
}
//监听器业务逻辑
private OperatorListener getListener() {
    return (wrapper, eventEnum) -> {
        if (eventEnum == OperatorEventEnum.START) {
            System.out.println("Op1 start...");
        }
        if (eventEnum == OperatorEventEnum.SUCCESS) {
            System.out.println("Op1 success...");
        }
        if (eventEnum == OperatorEventEnum.ERROR) {
            System.out.println("Op1 error...");
        }
    };
}
```
##### 14. 节点组

![vfdGnA.png](https://s1.ax1x.com/2022/09/06/vHm1bV.png)

OP节点组，将多个节点抽象成一个组，可以简化节点依赖的管理，尤其是当整个DAG中节点比较多时，根据依赖关系划分成多个组，每个组内的节点可以单独管理，比如系统中涉及多个模块，每个模块又有多个OP节点

代码示例：
```
public void test() {
    DagEngine engine = new DagEngine(executor);
    OperatorWrapperGroup group1 = buildGroup1(engine);	//构造节点组1
    OperatorWrapperGroup group2 = buildGroup2(engine);	//构造节点组1
    group1.next(group2.getGroupBeginId());	//节点组1的下一个节点是节点组2的开始节点
    group2.next("9");	//节点组2的下一个节点是普通节点

    OperatorWrapper<Integer, Integer> wrapper9 = new OperatorWrapper<Integer, Integer>()
            .id("9")
            .engine(engine)
            .operator(operator9)
            .next("10")
            ;
    OperatorWrapper<Integer, Integer> wrapper10 = new OperatorWrapper<Integer, Integer>()
            .id("10")
            .engine(engine)
            .operator(operator10)
            ;
    engine.runAndWait(3000, group1.getGroupBeginId());	//指定引擎执行的开始节点是节点组1
}
//节点组1
private OperatorWrapperGroup buildGroup1(DagEngine engine) {
    OperatorWrapper<Integer, Integer> wrapper1 = new OperatorWrapper<Integer, Integer>()
            .id("1")
            .engine(engine)
            .operator(operator1)
            ;
    OperatorWrapper<Integer, Integer> wrapper2 = new OperatorWrapper<Integer, Integer>()
            .id("2")
            .engine(engine)
            .operator(operator2)
            .depend("1")
            ;
    OperatorWrapper<Integer, Integer> wrapper3 = new OperatorWrapper<Integer, Integer>()
            .id("3")
            .engine(engine)
            .operator(operator3)
            .depend("1")
            ;
    OperatorWrapper<Integer, Integer> wrapper4 = new OperatorWrapper<Integer, Integer>()
            .id("4")
            .engine(engine)
            .operator(operator4)
            .depend("1")
            ;

    return new OperatorWrapperGroup(engine)
            .beginWrapperIds("1")	//封装节点组的开始节点
            .endWrapperIds("2", "3", "4")	//封装节点组的结束节点
            .init()
            ;
}
//节点组2
private OperatorWrapperGroup buildGroup2(DagEngine engine) {
    OperatorWrapper<Integer, Integer> wrapper5 = new OperatorWrapper<Integer, Integer>()
            .id("5")
            .engine(engine)
            .operator(operator5)
            ;
    OperatorWrapper<Integer, Integer> wrapper6 = new OperatorWrapper<Integer, Integer>()
            .id("6")
            .engine(engine)
            .operator(operator6)
            .depend("5")
            ;
    OperatorWrapper<Integer, Integer> wrapper7 = new OperatorWrapper<Integer, Integer>()
            .id("7")
            .engine(engine)
            .operator(operator7)
            .depend("5")
            ;
    OperatorWrapper<Integer, Integer> wrapper8 = new OperatorWrapper<Integer, Integer>()
            .id("8")
            .engine(engine)
            .operator(operator8)
            .depend("5")
            ;

    return new OperatorWrapperGroup(engine)
            .beginWrapperIds("5")	//封装节点组的开始节点
            .endWrapperIds("6", "7", "8")	//封装节点组的结束节点
            .init()
            ;
}
```
##### 15. 自定义中断

![vfdGnA.png](https://s1.ax1x.com/2022/08/29/vfBDje.png)

流程执行过程中，根据当前执行的结果可以随时中断流程的执行

如上图，如果节点2执行完后不满足相关条件，可以直接将节点2设置成结束节点，DAG引擎执行过程中会自动判断，执行到结束节点后会结束整个流程
```
OperatorWrapper<Integer, Integer> wrapper1 = new OperatorWrapper<Integer, Integer>()
        .id("1")
        .engine(engine)
        .operator(operator1)
        .next("2")
        ;
OperatorWrapper<Integer, Integer> wrapper2 = new OperatorWrapper<Integer, Integer>()
        .id("2")
        .engine(engine)
        .operator(operator2)
        .next("3")
        .after((w) -> {
            //将当前节点设置为结束节点
            DagEngine.stopAt(w);
        })
        ;
OperatorWrapper<Integer, Integer> wrapper3 = new OperatorWrapper<Integer, Integer>()
        .id("3")
        .engine(engine)
        .operator(operator3)
        ;
```
#### 三、详细代码示例
见 [task-example](./taskflow-example)
