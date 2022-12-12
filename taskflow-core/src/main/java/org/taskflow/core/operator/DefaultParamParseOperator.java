package org.taskflow.core.operator;

import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.taskflow.common.util.gson.GsonUtil;
import org.taskflow.config.op.OpConfig;
import org.taskflow.config.op.ParamExpressUtil;
import org.taskflow.config.op.ParamParserHelper;
import org.taskflow.config.op.param.JsonPathConfig;
import org.taskflow.config.op.param.OpParamConfig;
import org.taskflow.config.op.param.ParsedParam;
import org.taskflow.core.DagContextHolder;
import org.taskflow.core.exception.TaskFlowException;
import org.taskflow.core.wrapper.OperatorWrapper;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 默认的OP入参解析器
 * 将参数作为一个整体进行解析，不解析子字段，使用 Gson 序列化/反序列化
 * Created by ytyht226 on 2022/11/22.
 */
@Slf4j
@SuppressWarnings("all")
public class DefaultParamParseOperator implements IParamParseOperator<OperatorWrapper, Object> {

    @Override
    public Object execute(OperatorWrapper param) throws Exception {
        Object result = null;
        try {
            OpConfig opConfig = GsonUtil.fromJson(param.getOpConfig(), OpConfig.class);

            Object proxyObj = param.getProxyObj();
            String proxyObjName = opConfig.getOpParamConfig().getProxyObjName();
            if (proxyObj == null && StringUtils.isBlank(proxyObjName)) {
                throw new TaskFlowException("jsonPathConfig error");
            }
            if (proxyObj == null) {
                //待实现：proxyObj为空时，根据 proxyObjName 从spring容器中获取目标对象，等后续完善好dsl后再实现
                throw new TaskFlowException("proxyObj is null");
            }
            OpParamConfig opParamConfig = opConfig.getOpParamConfig();
            opParamConfig.setProxyObjName(proxyObjName);
            List<JsonPathConfig> jsonPathList = opParamConfig.getJsonPathList();
            ParsedParam parsedParam = parseJsonPath(jsonPathList);

            Method targetMethod = proxyObj.getClass().getDeclaredMethod(opParamConfig.getMethodName(), parsedParam.getParameterTypes());
            result = targetMethod.invoke(proxyObj, parsedParam.getArgs());
        } catch (Exception e) {
            log.error("ParamParserOperator execute error", e);
            throw e;
        }
        return result;
    }

    private ParsedParam parseJsonPath(List<JsonPathConfig> jsonPathList) throws Exception {
        ParsedParam parsedParam = new ParsedParam();
        Class<?>[] parameterTypes = new Class[jsonPathList.size()];
        Object[] args = new Object[jsonPathList.size()];
        parsedParam.setParameterTypes(parameterTypes);
        parsedParam.setArgs(args);

        for (int i = 0; i < jsonPathList.size(); i++) {
            JsonPathConfig jsonPathConfig = jsonPathList.get(i);

            Class<?> typeClass = Class.forName(jsonPathConfig.getType());
            jsonPathConfig.setTypeClass(typeClass);
            parameterTypes[i] = typeClass;
            args[i] = parseArg(jsonPathConfig);
        }
        return parsedParam;
    }

    /**
     * 解析逻辑
     * 1、path 不存在，直接返回 defaultValue
     * 2、path、defaultValue 都存在，根据 path 解析报错，直接返回 defaultValue
     * 3、path 存在，defaultValue 不存在，根据 path 解析报错，抛异常
     * @param jsonPathConfig
     * @return
     */
    public Object parseArg(JsonPathConfig jsonPathConfig) {
        String path = jsonPathConfig.getPath();
        Object defaultValue = jsonPathConfig.getValue();
        Class<?> typeClass = jsonPathConfig.getTypeClass();
        //path value都为空，配置有误，抛异常
        if (StringUtils.isBlank(path) && defaultValue == null) {
            throw new TaskFlowException("jsonPathConfig error");
        }
        //path为空直接返回value
        if (StringUtils.isBlank(path)) {
            return convertValue(typeClass, defaultValue);
        }
        //从请求上下文或其它OP结果中根据jsonpath解析
        //解析jsonpath异常时，如果 value 不为空则直接返回，否则抛异常
        Object value;
        try {
            Pair<String, String> pair = ParamExpressUtil.parseJsonPath(path);
            String opId = pair.getKey();
            String realPath = pair.getValue();
            OperatorResult operatorResult = DagContextHolder.getOperatorResult(opId);
            Object result = operatorResult.getResult();
            value = ParamParserHelper.parse(GsonUtil.toJson(result), realPath, typeClass);
        } catch (Exception e) {
            log.error("parseArg error", e);
            if (defaultValue != null) {
                return convertValue(typeClass, defaultValue);
            } else {
                throw new TaskFlowException(e);
            }
        }
        return value;
    }

    /**
     * 将 value 转换成具体类型的对象
     */
    public Object convertValue(Class<?> typeClass, Object value) {
        return GsonUtil.convertValue(typeClass, value);
    }
}