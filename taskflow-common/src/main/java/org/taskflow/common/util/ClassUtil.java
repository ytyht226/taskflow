package org.taskflow.common.util;

import org.springframework.util.ClassUtils;

/**
 * Created by ytyht226 on 2022/11/28.
 */
public class ClassUtil {

    public static boolean isPrimitive(Class<?> type) {
        return ClassUtils.isPrimitiveOrWrapper(type) || type == String.class;
    }
}