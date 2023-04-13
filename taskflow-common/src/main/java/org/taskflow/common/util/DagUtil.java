package org.taskflow.common.util;


import org.taskflow.common.constant.DagConstant;

/**
 * Created by ytyht226 on 2023/3/1.
 */
public class DagUtil {
    /**
     * 当前节点是否节点组
     */
    public static boolean isGroupId(String id) {
        return id != null && id.startsWith(DagConstant.OP_GROUP_PREFIX);
    }
    /**
     * 当前节点是否节点组中的开始节点
     */
    public static boolean isGroupBeginOp(String id) {
        return id != null && id.startsWith(DagConstant.BEGIN_OP_IN_GROUP_PREFIX);
    }
    /**
     * 当前节点是否节点组中的结束节点
     */
    public static boolean isGroupEndOp(String id) {
        return id != null && id.startsWith(DagConstant.END_OP_IN_GROUP_PREFIX);
    }
}