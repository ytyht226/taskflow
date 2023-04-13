package org.taskflow.example.task;

import lombok.Data;

/**
 * Created by ytyht226 on 2023/3/17.
 */
@Data
public class ModelResult {
    private int code;
    private String msg;

    private Model model;
}