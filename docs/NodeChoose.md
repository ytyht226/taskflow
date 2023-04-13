### 节点选择
#### 背景
在某些场景下需要根据节点执行结果执行不同的后续节点，在图中的表现是有分叉，执行分叉的节点后再执行共享的流程，在图中的表现是有合并节点，这种执行情况不同于分支选择
如下图所示是分支选择的例子，节点1、6是分支选择节点，最终执行的路径可能是1->3->6->9，分支选择之后的流程不能出现合并节点

![vfdGnA.png](https://s1.ax1x.com/2023/04/13/ppvx41K.png)

如下图是节点选择的例子，节点1是节点选择节点，在后续节点(2、3、4)中选择要执行的节点，最终执行路径可能是1->3->5->6，节点5是合并节点，后续的流程是共享的，不管节点1选择的后续节点是哪个，节点5之后的节点都会执行

![vfdGnA.png](https://s1.ax1x.com/2023/04/13/ppvvPKg.png)

#### 节点选择类型
##### 1、节点&节点
这种场景比较简单，待选择的都是单一节点，如下图所示

![vfdGnA.png](https://s1.ax1x.com/2023/04/13/ppvzC7j.png)

代码示例请参考：[代码示例](../taskflow-example/src/main/java/org/taskflow/example/choose/op/op_op)
##### 2、节点组&节点

参与选择的部分过程比较复杂，需要使用节点组圈定好需要参与选择的节点，其它待选择的就是单一的节点，如下图所示

![vfdGnA.png](https://s1.ax1x.com/2023/04/13/ppvzuB4.png)

代码示例请参考：[代码示例](../taskflow-example/src/main/java/org/taskflow/example/choose/op/group_op)
##### 3、节点组&节点组

要参与选择的过程都比较复杂，需要使用节点组圈定好需要参与选择的节点，如下图所示

![vfdGnA.png](https://s1.ax1x.com/2023/04/13/ppvzNuD.png)

代码示例请参考：[代码示例](../taskflow-example/src/main/java/org/taskflow/example/choose/op/group_group)