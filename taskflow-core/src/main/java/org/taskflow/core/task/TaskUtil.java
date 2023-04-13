package org.taskflow.core.task;


import org.taskflow.core.DagEngine;
import org.taskflow.core.enums.ResultState;
import org.taskflow.core.operator.IOperator;
import org.taskflow.core.operator.OperatorResult;
import org.taskflow.core.wrapper.OperatorWrapper;
import org.taskflow.core.wrapper.OperatorWrapperGroup;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 任务工具类
 * 1、将一批任务并行执行
 * 2、将一批任务分批次执行（每个批次包含多个任务，并行执行）
 * 3、将一批任务分批次执行（每个批次是一个任务，每个任务可以传入多个参数；比如调用的批量接口）
 * Created by ytyht226 on 2023/3/16.
 */
@SuppressWarnings("all")
public class TaskUtil {
    /**
     * 单个任务wrapper的id
     */
    public static final String SINGLE_TASK_PREFIX = "single_task_";
    /**
     * 节点组任务wrapper的id
     */
    public static final String GROUP_TASK_PREFIX = "batch_task_";
    /**
     * 单个任务的批次序列号
     */
    private static final AtomicLong SINGLE_GROUP_SEQ = new AtomicLong(1);
    /**
     * 多个任务的批次序列号
     */
    private static final AtomicLong GROUP_TASK_SEQ = new AtomicLong(1);

    /**
     * 将一批任务并行执行
     */
    public static <P,V> List<TaskResult<P, V>> parallelTask(List<P> taskList, IOperator operator, ExecutorService executor, long timeout) {
        return serialOrParallelTask(taskList, false, operator, executor, timeout, true);
    }

    /**
     * 将一批任务串行执行
     */
    public static <P,V> List<TaskResult<P, V>> serialTask(List<P> taskList, IOperator operator, ExecutorService executor, long timeout) {
        return serialOrParallelTask(taskList,true, operator, executor, timeout, true);
    }

    /**
     * 将一批任务串行或并行执行
     * 如果执行超时，任务会被中断，TaskResult 中的 data 内容为 null
     * @param taskList      任务列表
     * @param isSerial      串行：true，并行：false
     * @param operator      执行任务的逻辑
     * @param executor      线程池
     * @param timeout       超时时间，单位：毫秒
     * @param parseResult   是否要解析任务结果，默认是，解析后可以直接根据data字段获取，否则需要根据 operatorWrapper 解析
     * @param <P>           执行任务的入参类型
     * @param <V>           任务执行结果类型
     * @return  任务结果集
     */
    public static <P,V> List<TaskResult<P, V>> serialOrParallelTask(List<P> taskList, boolean isSerial, IOperator operator, ExecutorService executor, long timeout, boolean parseResult) {
        if (taskList == null || taskList.size() == 0) {
            return null;
        }
        List<TaskResult<P, V>> resultList = new ArrayList<>(taskList.size());

        DagEngine engine = new DagEngine(executor);
        long currSingleGroup = SINGLE_GROUP_SEQ.getAndIncrement();
        int index = 1;
        for (P obj : taskList) {
            String id = SINGLE_TASK_PREFIX + currSingleGroup + "_" + index;
            OperatorWrapper<P, V> wrapper = new OperatorWrapper<>()
                    .id(id)
                    .context(obj)
                    .operator(operator)
                    .engine(engine);

            if (isSerial) {
                String nextId = SINGLE_TASK_PREFIX + currSingleGroup + "_" + (index + 1);
                //index从1开始
                if (index < taskList.size()) {
                    wrapper.next(nextId);
                }
            }
            TaskResult<P, V> taskResult = new TaskResult<>(obj, wrapper);
            resultList.add(taskResult);
            index++;
        }
        //引擎执行
        engine.runAndWait(timeout);
        //解析结果
        parseWrapperResult(resultList, parseResult);
        return resultList;
    }

    /**
     * 将一批任务分批次执行（每个批次包含多个任务，并行执行，每个任务只有一个参数）
     * 每个批次内的任务是并行执行，不同批次之间是串行执行
     */
    public static <P,V> List<TaskResult<P, V>> serialBatchTask(List<P> taskList, int batchSize, IOperator operator, ExecutorService executor, long timeout) {
        return serialBatchTask(taskList, batchSize, operator, executor, timeout, true);
    }

    /**
     * 将一批任务分批次执行（每个批次包含多个任务，并行执行，每个任务只有一个参数）
     * 每个批次内的任务是并行执行，不同批次之间是串行执行
     * @param taskList      任务列表
     * @param batchSize     每个批次包含多少任务
     * @param operator      执行任务的逻辑
     * @param executor      线程池
     * @param timeout       超时时间，单位：毫秒
     * @param <P>           执行任务的入参类型
     * @param <V>           任务执行结果类型
     * @return  任务结果集
     */
    public static <P,V> List<TaskResult<P, V>> serialBatchTask(List<P> taskList, int batchSize, IOperator operator, ExecutorService executor, long timeout, boolean parseResult) {
        if (taskList == null || taskList.size() == 0) {
            return null;
        }

        DagEngine engine = new DagEngine(executor);
        List<TaskResult<P, V>> resultList = new ArrayList<>(taskList.size());

        //按批次将数据分组
        LinkedList<List<P>> batchGroupList = new LinkedList<>();
        batchGroupList.add(new ArrayList<>());

        int counter = 1;
        for (P obj : taskList) {
            if (counter > batchSize) {
                counter = 1;
                batchGroupList.add(new ArrayList<>());
            }
            List<P> currGroup = batchGroupList.getLast();
            currGroup.add(obj);
            counter++;
        }
        //将批次划分节点组（WrapperGroup）
        LinkedList<OperatorWrapperGroup> batchWrapperGroupList = new LinkedList<>();
        for (List<P> currGroup : batchGroupList) {
            batchWrapperGroupList.add(buildBatchGroup(engine, operator, resultList, currGroup));
        }
        //按执行顺序指定节点组依赖
        for (int i = 0; i < batchWrapperGroupList.size(); i++) {
            OperatorWrapperGroup currWrapperGroup = batchWrapperGroupList.get(i);
            if (i < batchWrapperGroupList.size() - 1) {
                currWrapperGroup.next(batchWrapperGroupList.get(i + 1).getGroupBeginId());
            }
        }

        //引擎执行
        engine.runAndWait(timeout);
        //解析结果
        parseWrapperResult(resultList, parseResult);
        return resultList;
    }

    /**
     * 将一批任务分批次执行（每个批次是一个任务，每个任务可以传入多个参数；比如调用的批量接口），不同批次之间串行执行
     * @param taskList      任务列表
     * @param paramSize     执行任务时，传入参数的个数，框架根据该参数大小计算要拆分成多少个批次
     * @param operator      执行任务的逻辑
     * @param executor      线程池
     * @param timeout       超时时间，单位：毫秒
     * @param <P>           执行任务的入参类型
     * @param <V>           任务执行结果类型
     * @return  任务结果集
     */
    public static <P,V> List<MultiParamTaskResult<P, V>> serialBatchMultiParamTask(List<P> taskList, int paramSize, IOperator operator, ExecutorService executor, long timeout) {
        return batchMultiParamTask(taskList, paramSize, true, operator, executor, timeout);
    }

    /**
     * 将一批任务分批次执行（每个批次是一个任务，每个任务可以传入多个参数；比如调用的批量接口），不同批次之间并行执行
     * @param taskList      任务列表
     * @param paramSize     执行任务时，传入参数的个数，框架根据该参数大小计算要拆分成多少个批次
     * @param operator      执行任务的逻辑
     * @param executor      线程池
     * @param timeout       超时时间，单位：毫秒
     * @param <P>           执行任务的入参类型
     * @param <V>           任务执行结果类型
     * @return  任务结果集
     */
    public static <P,V> List<MultiParamTaskResult<P, V>> parallelBatchMultiParamTask(List<P> taskList, int paramSize, IOperator operator, ExecutorService executor, long timeout) {
        return batchMultiParamTask(taskList, paramSize, false, operator, executor, timeout);
    }

    private static <P, V> OperatorWrapperGroup buildBatchGroup(DagEngine engine, IOperator operator, List<TaskResult<P, V>> resultList, List<P> currGroup) {
        Set<String> wrapperIdSet = new HashSet<>(currGroup.size());
        long currBatchGroup = GROUP_TASK_SEQ.getAndIncrement();
        int index = 1;
        for (P curr : currGroup) {
            String id = GROUP_TASK_PREFIX + currBatchGroup + "_" + index;
            wrapperIdSet.add(id);
            OperatorWrapper<P, V> wrapper = new OperatorWrapper<>()
                    .id(id)
                    .context(curr)
                    .operator(operator)
                    .engine(engine);
            TaskResult<P, V> result = new TaskResult<>(curr, wrapper);
            resultList.add(result);
            index++;
        }

        String[] wrapperIdArray = wrapperIdSet.toArray(new String[0]);
        return new OperatorWrapperGroup(engine)
                .beginWrapperIds(wrapperIdArray)
                .endWrapperIds(wrapperIdArray)
                .init()
                ;
    }

    /**
     * 解析wrapper结果
     */
    private static  <P, V> void parseWrapperResult(List<TaskResult<P, V>> resultList, boolean parseResult) {
        if (resultList == null || resultList.size() == 0 || !parseResult) {
            return;
        }
        for (TaskResult<P, V> taskResult : resultList) {
            if (taskResult.getOperatorWrapper().getOperatorResult().getResultState() == ResultState.SUCCESS) {
                taskResult.setData(taskResult.getOperatorWrapper().getOperatorResult().getResult());
            }
        }
    }

    /**
     * 将一批任务分批次执行（每个批次是一个任务，每个任务可以传入多个参数；比如调用的批量接口），不同批次之间可以串行或并行
     * @param taskList      任务列表
     * @param paramSize     执行任务时，传入参数的个数，框架根据该参数大小计算要拆分成多少个批次
     * @param isSerial      不同批次之间是串行或并行，串行：true，并行：false
     * @param operator      执行任务的逻辑
     * @param executor      线程池
     * @param timeout       超时时间，单位：毫秒
     * @param <P>           执行任务的入参类型
     * @param <V>           任务执行结果类型
     * @return  任务结果集
     */
    private static <P,V> List<MultiParamTaskResult<P, V>> batchMultiParamTask(List<P> taskList, int paramSize, boolean isSerial, IOperator operator, ExecutorService executor, long timeout) {
        if (taskList == null || taskList.size() == 0) {
            return null;
        }

        DagEngine engine = new DagEngine(executor);
        List<MultiParamTaskResult<P, V>> resultList = new ArrayList<>(taskList.size());

        //按批次将数据分组
        LinkedList<List<P>> batchGroupList = new LinkedList<>();
        batchGroupList.add(new ArrayList<>());

        int counter = 1;
        for (P obj : taskList) {
            if (counter > paramSize) {
                counter = 1;
                batchGroupList.add(new ArrayList<>());
            }
            List<P> currGroup = batchGroupList.getLast();
            currGroup.add(obj);
            counter++;
        }
        //每个数据分组创建一个Wrapper
        List<OperatorWrapper<List<P>, List<MultiParamTaskResult<P, V>>>> wrapperList = new ArrayList<>(batchGroupList.size());
        Map<OperatorWrapper<List<P>, List<MultiParamTaskResult<P, V>>>, List<P>> wrapperResultMap = new HashMap<>(batchGroupList.size());

        long currSingleGroup = SINGLE_GROUP_SEQ.getAndIncrement();
        int index = 1;
        for (List<P> currGroup : batchGroupList) {
            //每个wrapper对应的执行结果
            String id = SINGLE_TASK_PREFIX + currSingleGroup + "_" + index;
            OperatorWrapper<List<P>, List<MultiParamTaskResult<P, V>>> wrapper = new OperatorWrapper<>()
                    .id(id)
                    .context(currGroup)
                    .operator(operator)
                    .engine(engine);

            if (isSerial) {
                //按执行顺序指定批次依赖
                String nextId = SINGLE_TASK_PREFIX + currSingleGroup + "_" + (index + 1);
                //index从1开始
                if (index < batchGroupList.size()) {
                    wrapper.next(nextId);
                }
            }

            wrapperList.add(wrapper);
            wrapperResultMap.put(wrapper, currGroup);
            index++;
        }

        //引擎执行
        engine.runAndWait(timeout);
        //将当前任务返回结果列表拆解后添加到结果集
        for (OperatorWrapper<List<P>, List<MultiParamTaskResult<P, V>>> wrapper : wrapperList) {
            // 如果当前任务执行成功，直接将返回结果添加到结果集
            if (wrapper.getOperatorResult().getResultState() == ResultState.SUCCESS) {
                OperatorResult<List<MultiParamTaskResult<P, V>>> operatorResult = wrapper.getOperatorResult();
                if (operatorResult == null || operatorResult.getResult() == null && operatorResult.getResult().size() == 0) {
                    resultList.addAll(operatorResult.getResult());
                } else {
                    for (MultiParamTaskResult<P, V> singleResult : operatorResult.getResult()) {
                        singleResult.setOperatorWrapper(wrapper);
                        resultList.add(singleResult);
                    }
                }
            }
            // 如果当前任务执行失败，构造空结果添加到结果集，保证结果集的顺序和 taskList 一致
            else {
                List<P> taskBatchList = wrapperResultMap.get(wrapper);
                for (int i = 0; i < taskBatchList.size(); i++) {
                    MultiParamTaskResult<P, V> singleResult = new MultiParamTaskResult(taskBatchList.get(i), null, wrapper);
                    resultList.add(singleResult);
                }
            }
        }
        return resultList;
    }
}