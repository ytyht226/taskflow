package org.taskflow.core.operator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.util.Pair;
import org.taskflow.common.util.ClassUtil;
import org.taskflow.common.util.gson.GsonUtil;
import org.taskflow.config.op.ParamExpressUtil;
import org.taskflow.config.op.ParamParserHelper;
import org.taskflow.core.DagContextHolder;

import java.util.Map;

/**
 * OP入参解析器
 * 递归地解析参数的子字段
 * Created by ytyht226 on 2022/11/28.
 */
public class RecurseParamParseOperator extends DefaultParamParseOperator {

    @Override
    public Object convertValue(Class<?> typeClass, Object value) {
        if (ClassUtil.isPrimitive(typeClass)) {
            return super.convertValue(typeClass, value);
        }
        JsonElement rootElement = GsonUtil.getGson().toJsonTree(value);

        Object processedValue = processValue(rootElement);
        return super.convertValue(typeClass, processedValue);
    }

    /**
     * 递归解析节点内容
     */
    private Object processValue(JsonElement element) {

        if (element.isJsonObject()) {
            JsonObject objElement = (JsonObject) element;
            for (Map.Entry<String, JsonElement> entry : ((JsonObject) element).entrySet()) {
                Object value = processValue(entry.getValue());
                objElement.add(entry.getKey(), (JsonElement) value);
            }
        } else if (element.isJsonArray()) {
            JsonArray arrElement = (JsonArray) element;
            for (int i = 0; i < arrElement.size(); i++) {
                Object value = processValue(arrElement.get(i));
                arrElement.set(i, (JsonElement)value);
            }
        } else {
            String path = element.getAsString();
            if (!ParamExpressUtil.isJsonPathExpression(path)) {
                return element;
            }
            Object parsedValue = parsePathValue(path);
            return parsedValue;
        }
        return element;
    }

    /**
     * 根据path从引擎上下文中解析具体内容
     */
    private Object parsePathValue(String path) {
        Pair<String, String> pair = ParamExpressUtil.parseJsonPath(path);
        String opId = pair.getKey();
        String realPath = pair.getValue();
        OperatorResult<?> operatorResult = DagContextHolder.getOperatorResult(opId);
        Object result = operatorResult.getResult();
        return ParamParserHelper.parse(GsonUtil.toJson(result), realPath);
    }
}