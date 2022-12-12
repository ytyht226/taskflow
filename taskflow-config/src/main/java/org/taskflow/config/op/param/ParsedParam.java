package org.taskflow.config.op.param;

import lombok.Data;

/**
 * 解析后的运行时参数信息
 * Created by ytyht226 on 2022/11/23.
 */
@Data
public class ParsedParam {
    /**
     * 参数类型
     */
    private Class<?>[] parameterTypes;
    /**
     * 参数值
     */
    private Object[] args;
}