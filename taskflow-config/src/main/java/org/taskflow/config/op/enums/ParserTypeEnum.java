package org.taskflow.config.op.enums;


import org.taskflow.config.op.parser.GsonParamParser;
import org.taskflow.config.op.parser.IParamParser;

/**
 * 参数解析器类型枚举
 * Created by ytyht226 on 2022/11/22.
 */
public enum ParserTypeEnum {
    GSON;

    public static IParamParser getParserByType(ParserTypeEnum typeEnum) {
        switch (typeEnum) {
            case GSON:
            default:
                return GsonParamParser.getInstance();
        }
    }
}
