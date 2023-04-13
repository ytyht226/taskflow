package org.taskflow.example.task;

import org.taskflow.core.operator.IOperator;
import org.taskflow.core.task.MultiParamTaskResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ytyht226 on 2023/3/16.
 */
public class BatchModelOperator implements IOperator<List<Model>, List<MultiParamTaskResult<Model, ModelResult>>> {
    @Override
    public List<MultiParamTaskResult<Model, ModelResult>> execute(List<Model> param) throws Exception {
//        int a = 5 / 0;
        List<MultiParamTaskResult<Model, ModelResult>> taskResults = new ArrayList<>();
        for (Model model : param) {
            model.setName("name_" + model.getId());
            ModelResult modelResult = new ModelResult();
            modelResult.setCode(200);
            modelResult.setMsg("SUCC");
            modelResult.setModel(model);

            MultiParamTaskResult<Model, ModelResult> taskResult = new MultiParamTaskResult<>(model, modelResult);
            taskResults.add(taskResult);
        }
        return taskResults;
    }
}