package org.taskflow.example.task;

import org.taskflow.core.operator.IOperator;

/**
 * Created by ytyht226 on 2023/3/16.
 */
public class ModelOperator implements IOperator<Model, ModelResult> {
    @Override
    public ModelResult execute(Model param) throws Exception {

        ModelResult modelResult = new ModelResult();
        modelResult.setCode(200);
        modelResult.setMsg("SUCC");
        modelResult.setModel(param);
        return modelResult;
    }
}