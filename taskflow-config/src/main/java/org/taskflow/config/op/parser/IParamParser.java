package org.taskflow.config.op.parser;

import java.util.List;

/**
 * Created by ytyht226 on 2022/11/21.
 */
public interface IParamParser {

    Object parse(String context, String path);

    <T> T parse(String context, String path, Class<T> type);

    List<Object> parse(String context, List<String> pathList);
}
