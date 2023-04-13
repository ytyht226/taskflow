package org.taskflow.example.task;

import org.taskflow.core.task.MultiParamTaskResult;
import org.taskflow.core.task.TaskResult;
import org.taskflow.core.task.TaskUtil;
import org.taskflow.core.thread.pool.CustomThreadPool;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Created by ytyht226 on 2023/3/15.
 */
public class TaskTest {
    ExecutorService executor = CustomThreadPool.newFixedThreadPoolWrapper(50);
    IntegerOperator integerOperator = new IntegerOperator();
    ModelOperator modelOperator = new ModelOperator();
    BatchIntegerOperator batchIntegerOperator = new BatchIntegerOperator();
    BatchModelOperator batchModelOperator = new BatchModelOperator();

    //任务列表
    List<Integer> idList = new ArrayList<>();
    List<Model> modelList = new ArrayList<>();

    @Before
    public void setUp() {
        for (int i = 1; i <= 100; i++) {
            idList.add(i);

            Model model = new Model(i);
            modelList.add(model);
        }
    }

    /**
     * 一个批次执行，task任务入参、出参都是简单的基本数据类型
     */
    @Test
    public void parallelTaskTest1() {
        long begin = System.currentTimeMillis();
        List<TaskResult<Integer, Integer>> results = TaskUtil.parallelTask(idList, integerOperator, executor, 300_000);
        long end = System.currentTimeMillis();
        System.out.println("cost: " + (end - begin));
        for (TaskResult<Integer, Integer> result : results) {
            System.out.println(result);
        }
    }

    /**
     * 一个批次执行，task任务入参、出参是自定义类型
     */
    @Test
    public void parallelTaskTest2() {
        long begin = System.currentTimeMillis();
        List<TaskResult<Model, ModelResult>> results = TaskUtil.parallelTask(modelList, modelOperator, executor, 300_000);
        long end = System.currentTimeMillis();
        System.out.println("cost: " + (end - begin));
        for (TaskResult<Model, ModelResult> result : results) {
            System.out.println(result);
        }
    }

    @Test
    public void serialTaskTest1() {
        long begin = System.currentTimeMillis();
        List<TaskResult<Integer, Integer>> results = TaskUtil.serialTask(idList, integerOperator, executor, 300_000);
        long end = System.currentTimeMillis();
        System.out.println("cost: " + (end - begin));
        for (TaskResult<Integer, Integer> result : results) {
            System.out.println(result);
        }
    }

    /**
     * 将一组任务分多个批次执行，task任务入参、出参都是简单的基本数据类型
     */
    @Test
    public void batchTaskTest1() {
        long begin = System.currentTimeMillis();
        List<TaskResult<Integer, Integer>> results = TaskUtil.serialBatchTask(idList, 2, integerOperator, executor, 300_000);
        long end = System.currentTimeMillis();
        System.out.println("cost: " + (end - begin));
        for (TaskResult<Integer, Integer> result : results) {
            System.out.println(result);
        }
    }

    /**
     * 将一组任务分多个批次执行，task任务入参、出参是自定义类型
     */
    @Test
    public void batchTaskTest2() {
        long begin = System.currentTimeMillis();
        List<TaskResult<Model, ModelResult>> results = TaskUtil.serialBatchTask(modelList, 2, modelOperator, executor, 300_000);
        long end = System.currentTimeMillis();
        System.out.println("cost: " + (end - begin));
        for (TaskResult<Model, ModelResult> result : results) {
            System.out.println(result);
        }
    }

    /**
     * 将一批任务分批次执行（每个批次是一个任务，每个任务可以传入多个参数；比如调用的批量接口）
     * 批次之间串行
     */
    @Test
    public void serialBatchMultiParamTask1() {
        long begin = System.currentTimeMillis();
        List<MultiParamTaskResult<Integer, Model>> results = TaskUtil.serialBatchMultiParamTask(idList, 2, batchIntegerOperator, executor, 300_000);
        long end = System.currentTimeMillis();
        System.out.println("cost: " + (end - begin));
        for (MultiParamTaskResult<Integer, Model> result : results) {
            System.out.println(result);
        }
    }

    /**
     * 将一批任务分批次执行（每个批次是一个任务，每个任务可以传入多个参数；比如调用的批量接口）
     * 批次之间串行，task任务入参、出参是自定义类型
     */
    @Test
    public void serialBatchMultiParamTask2() {
        long begin = System.currentTimeMillis();
        List<MultiParamTaskResult<Model, ModelResult>> results = TaskUtil.serialBatchMultiParamTask(modelList, 50, batchModelOperator, executor, 300_000);
        long end = System.currentTimeMillis();
        System.out.println("cost: " + (end - begin));
        for (MultiParamTaskResult<Model, ModelResult> result : results) {
            System.out.println(result);
        }
    }

    /**
     * 将一批任务分批次执行（每个批次是一个任务，每个任务可以传入多个参数；比如调用的批量接口）
     * 批次之间并行
     */
    @Test
    public void parallelBatchMultiParamTask1() {
        long begin = System.currentTimeMillis();
        List<MultiParamTaskResult<Integer, Model>> results = TaskUtil.parallelBatchMultiParamTask(idList, 2, batchIntegerOperator, executor, 300_000);
        long end = System.currentTimeMillis();
        System.out.println("cost: " + (end - begin));
        for (MultiParamTaskResult<Integer, Model> result : results) {
            System.out.println(result);
        }
    }
}