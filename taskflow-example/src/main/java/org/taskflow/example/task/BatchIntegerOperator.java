package org.taskflow.example.task;


import org.taskflow.core.operator.IOperator;
import org.taskflow.core.task.MultiParamTaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by ytyht226 on 2023/3/16.
 */
public class BatchIntegerOperator implements IOperator<List<Integer>, List<MultiParamTaskResult<Integer, Model>>> {
    @Override
    public List<MultiParamTaskResult<Integer, Model>> execute(List<Integer> param) throws Exception {
        TimeUnit.SECONDS.sleep(1);
        List<MultiParamTaskResult<Integer, Model>> taskResults = new ArrayList<>();
        for (int id : param) {
            Model model = new Model(id, "name_" + id);
            MultiParamTaskResult<Integer, Model> result = new MultiParamTaskResult<>(id, model);
            taskResults.add(result);
        }
        return taskResults;
    }
}