package org.taskflow.config.op.param;

import lombok.Data;

/**
 * 参数配置
 * Created by ytyht226 on 2022/11/22.
 */
@Data
public class JsonPathConfig {
    /**
     * 支持两种取值方式：#、$
     * #：从外部变量取值
     * $：从其它OP返回结果中取值
     */
    private String path;
    /**
     * 参数类型，全限定名
     */
    private String type;
    /**
     * type类型
     */
    private transient Class<?> typeClass;
    /**
     * 默认值
     * 1、path 不存在，返回 value
     * 2、path、value 都存在，根据 path 解析报错时，返回 value
     * 3、path 存在，value 不存在，根据 path 解析报错，抛异常
     */
    private Object value;
}