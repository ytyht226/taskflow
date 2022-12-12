package org.taskflow.common.util.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;


public class GsonUtil {

    private static Gson gson = new GsonBuilder()
            .serializeNulls()
            .registerTypeAdapterFactory(new NullStringToEmptyAdapterFactory<>())    //（值为null转换为""）
            .create();

    private static Gson gsonPrettyPrint = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapterFactory(new NullStringToEmptyAdapterFactory<>())    //（值为null转换为""）
            .create();

    private static GsonJsonProvider gsonJsonProvider = new GsonJsonProvider(getGson());

    private GsonUtil() {
    }

    public static Gson getGson() {
        return gson;
    }

    public static GsonJsonProvider getGsonJsonProvider() {
        return gsonJsonProvider;
    }

    public static String toJson(Object src) {
        return gson.toJson(src);
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }

    public static String prettyPrint(Object src) {
        return gsonPrettyPrint.toJson(src);
    }

    /**
     * 将 value 转换成具体类型的对象
     */
    public static Object convertValue(Class<?> typeClass, Object value) {
        if (typeClass.isInstance(value)) {
            return value;
        }
        return GsonUtil.fromJson(GsonUtil.toJson(value), typeClass);
    }
}
