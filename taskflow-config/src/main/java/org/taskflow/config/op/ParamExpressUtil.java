package org.taskflow.config.op;

import javafx.util.Pair;
import org.apache.commons.lang3.StringUtils;
import org.taskflow.common.constant.DagConstant;

/**
 * 参数表达式解析工具
 * Created by ytyht226 on 2022/11/28.
 */
public class ParamExpressUtil {
    /** 从请求上下文取值标志 */
    public static final String REQUEST_CONTEXT_MARK = "#";

    /** 从依赖的OP执行结果取值标志 */
    public static final String DEPENDENT_MARK = "$";

    /** 属性分隔符 */
    public static final String PROPERTY_SEPARATOR = ".";


    public static boolean isJsonPathExpression(String path) {
        return StringUtils.isNotBlank(path) && (path.startsWith(REQUEST_CONTEXT_MARK) || path.startsWith(DEPENDENT_MARK));
    }

    /**
     * 解析表达式中的任务id、JsonPath表达式
     * @return Pair<String, String> key: 任务id, value: jsonpath表达式
     * @see <a href="https://github.com/json-path/JsonPath">JsonPath</a>
     */
    public static Pair<String, String> parseJsonPath(String path) {
        String opId;
        String realPath;
        if (path.startsWith("#")) {
            //eg: #.request.param
            opId = DagConstant.REQUEST_CONTEXT_ID;
            realPath = path.replace("#", "$");
        } else if (path.startsWith("$")) {
            //eg: $1.response.param
            if (!path.contains(".")) {
                opId = path.substring(1);
                realPath = "$";
            } else {
                int firstDotIndex = path.indexOf(".");
                opId = path.substring(1, firstDotIndex);
                realPath = "$" + path.substring(1 + opId.length());
            }
        } else {
            throw new RuntimeException("jsonPathConfig path error");
        }
        return new Pair<>(opId, realPath);
    }
}