package com.jrasp.api.util;

public class GaArrayUtils {

    public static <T> boolean isEmpty(T[] array) {
        return null == array
                || array.length == 0;
    }

    public static <T> boolean isNotEmpty(T[] array) {
        return !isEmpty(array);
    }

    public static <T> int getLength(T[] array) {
        return isNotEmpty(array)
                ? array.length
                : 0;
    }

}
