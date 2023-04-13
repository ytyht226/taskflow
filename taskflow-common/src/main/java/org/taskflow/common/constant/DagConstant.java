package org.taskflow.common.constant;

/**
 * Created by ytyht226 on 2022/11/28.
 */
public interface DagConstant {
    /**
     * 将请求上下文保存到 DagContext，根据该 id 获取
     */
    String REQUEST_CONTEXT_ID = "##request_context_id##";
    /**
     * 节点组id前缀
     */
    String OP_GROUP_PREFIX = "#op_group#_";
    /**
     * 节点组中开始节点id前缀
     */
    String BEGIN_OP_IN_GROUP_PREFIX = "#begin#";
    /**
     * 节点组中结束节点id前缀
     */
    String END_OP_IN_GROUP_PREFIX = "#end#";
}
