package org.taskflow.config.op.model;

import lombok.Data;

/**
 * 解析表达式中的任务id、JsonPath表达式
 * Created by ytyht226 on 2024/1/7.
 */
@Data
public class JsonPathModel {
    /**
     * 任务id
     */
    private String opId;
    /**
     * jsonpath表达式
     */
    private String realPath;
}