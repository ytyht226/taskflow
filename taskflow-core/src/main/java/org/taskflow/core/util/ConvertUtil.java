package org.taskflow.core.util;


import org.taskflow.core.wrapper.OperatorWrapper;

import java.util.Set;

/**
 * 类型转换工具
 * Created by ytyht226 on 2023/3/2.
 */
public class ConvertUtil {

    @SuppressWarnings("rawtypes")
    public static OperatorWrapper[] set2Array(Set<OperatorWrapper<?, ?>> wrapperSet) {
        if (wrapperSet == null || wrapperSet.size() == 0) {
            return new OperatorWrapper[0];
        }
        return wrapperSet.toArray(new OperatorWrapper[0]);
    }

}