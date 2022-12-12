package org.taskflow.config.op;


import org.taskflow.config.op.enums.ParserTypeEnum;
import org.taskflow.config.op.parser.IParamParser;

import java.util.List;

/**
 * 参数解析工具类，默认使用基于gson的json-path解析
 * Created by ytyht226 on 2022/11/22.
 */
public class ParamParserHelper {

    public static Object parse(String context, String path) {
        return parse(ParserTypeEnum.GSON, context, path);
    }

    public static List<Object> parse(String context, List<String> pathList) {
        return parse(ParserTypeEnum.GSON, context, pathList);
    }

    public static <T> T parse(String context, String path, Class<T> type) {
        return parse(ParserTypeEnum.GSON, context, path, type);
    }

    public static Object parse(ParserTypeEnum parserType, String context, String path) {
        IParamParser paramParser = ParserTypeEnum.getParserByType(parserType);
        return paramParser.parse(context, path);
    }

    public static List<Object> parse(ParserTypeEnum parserType, String context, List<String> pathList) {
        IParamParser paramParser = ParserTypeEnum.getParserByType(parserType);
        return paramParser.parse(context, pathList);
    }

    public static <T> T parse(ParserTypeEnum parserType, String context, String path, Class<T> type) {
        IParamParser paramParser = ParserTypeEnum.getParserByType(parserType);
        return paramParser.parse(context, path, type);
    }

}