package org.taskflow.config.op.parser;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.internal.DefaultsImpl;
import com.jayway.jsonpath.internal.ParseContextImpl;
import org.taskflow.common.util.gson.GsonUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于Gson的参数解析器
 * Created by ytyht226 on 2022/11/21.
 */
public class GsonParamParser implements IParamParser {
    private static Configuration jsonPathConfiguration = Configuration.builder().jsonProvider(GsonUtil.getGsonJsonProvider()).options(DefaultsImpl.INSTANCE.options()).build();
    private static GsonParamParser gsonParamParser = new GsonParamParser();

    private GsonParamParser(){
    }

    public static GsonParamParser getInstance() {
        return gsonParamParser;
    }

    @Override
    public Object parse(String context, String path) {
        DocumentContext jsonContext = documentContext(context);
        return jsonContext.read(path);
    }

    @Override
    public <T> T parse(String context, String path, Class<T> type) {
        DocumentContext jsonContext = documentContext(context);
        Object value = jsonContext.read(path);
        if (value instanceof String) {
            return (T) value;
        }
        return GsonUtil.fromJson(GsonUtil.toJson(value), type);
    }

    @Override
    public List<Object> parse(String context, List<String> pathList) {
        DocumentContext jsonContext = documentContext(context);
        List<Object> valueList = new ArrayList<>(pathList.size());
        for (String path : pathList) {
            valueList.add(jsonContext.read(path));
        }
        return valueList;
    }

    private DocumentContext documentContext(String context) {
        ParseContextImpl parseContext = new ParseContextImpl(jsonPathConfiguration);
        return parseContext.parse(context);
    }

}