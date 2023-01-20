package com.jrasp.agent.api.util;

import java.util.*;

/**
 * 命令参数支撑类
 *
 * @author jrasp
 */
public class ParamSupported {

    /**
     * 转换器(字符串到指定类型的转换器)
     *
     * @param <T> 转换目标类型
     */
    interface Converter<T> {

        /**
         * 转换字符串为目标类型
         *
         * @param string 字符串内容
         * @return 目标类型
         */
        T convert(String string);
    }

    // 转换器集合
    final static Map<Class<?>, Converter<?>> converterMap = new HashMap<Class<?>, Converter<?>>();

    static {

        // 转换为字符串
        regConverter(new Converter<String>() {
            @Override
            public String convert(String string) {
                return string;
            }
        }, String.class);

        // 转换为Long
        regConverter(new Converter<Long>() {
            @Override
            public Long convert(String string) {
                return Long.valueOf(string);
            }
        }, long.class, Long.class);

        // 转换为Double
        regConverter(new Converter<Double>() {
            @Override
            public Double convert(String string) {
                return Double.valueOf(string);
            }
        }, double.class, Double.class);

        // 转换为Integer
        regConverter(new Converter<Integer>() {
            @Override
            public Integer convert(String string) {
                return Integer.valueOf(string);
            }
        }, int.class, Integer.class);

        // 转换为字符串数组
        regConverter(new Converter<String[]>() {
            @Override
            public String[] convert(String string) {
                return string.split(",");
            }
        }, String[].class, String[].class);

        // 转换List
        regConverter(new Converter<List>() {
            @Override
            public List<String> convert(String string) {
                return Arrays.asList(string.split(","));
            }
        }, List.class, List.class);

        // 转换set
        regConverter(new Converter<Set>() {
            @Override
            public Set<String> convert(String string) {
                return new HashSet<String>(Arrays.asList(string.split(",")));
            }
        }, Set.class, Set.class);
    }

    /**
     * 注册类型转换器
     *
     * @param converter 转换器
     * @param typeArray 类型的Java类数组
     * @param <T>       类型
     */
    public static <T> void regConverter(Converter<T> converter, Class<T>... typeArray) {
        for (final Class<T> type : typeArray) {
            converterMap.put(type, converter);
        }
    }

    // 依据类型转换
    public static <T> T getParameter(Map<String, String> param, String name, Class<T> type, T defaultValue) {
        return getParameter(param, name, (Converter<T>) converterMap.get(type), defaultValue);
    }

    // 使用 converter转换
    public static <T> T getParameter(Map<String, String> param, String name, Converter<T> converter, T defaultValue) {
        String string = param.get(name);
        return !isBlank(string) ? converter.convert(string) : defaultValue;
    }

    public static boolean isBlank(final CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断数组是否为空
     *
     * @param array 数组
     * @param <T>   数组类型
     * @return TRUE:数组为空(null或length==0);FALSE:数组不为空
     */
    public static <T> boolean isEmpty(T[] array) {
        return null == array || array.length == 0;
    }

}
