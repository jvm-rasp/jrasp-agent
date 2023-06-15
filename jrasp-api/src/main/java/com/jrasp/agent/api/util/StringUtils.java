package com.jrasp.agent.api.util;

import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Set;

public class StringUtils {

    /**
     * 判断数组是否为空
     *
     * @param array 数组
     * @param <T>   数组类型
     * @return TRUE:数组为空(null或length==0);FALSE:数组不为空
     */
    public static <T> boolean isEmpty(T[] array) {
        return null == array
                || array.length == 0;
    }

    /**
     * 判断数组是否不为空
     *
     * @param array 数组
     * @param <T>   数组类型
     * @return TRUE:数组不为空;FALSE:数组为空(null或length==0)
     */
    public static <T> boolean isNotEmpty(T[] array) {
        return !isEmpty(array);
    }

    public static boolean isBlank(String s) {
        return s == null || s.length() == 0;
    }

    public static String join(Object[] array, String separator) {
        return array == null ? null : join(array, separator, 0, array.length);
    }

    public static String join(Object[] array, String separator, int startIndex, int endIndex) {
        if (array == null) {
            return null;
        } else {
            if (separator == null) {
                separator = "";
            }

            int noOfItems = endIndex - startIndex;
            if (noOfItems <= 0) {
                return "";
            } else {
                StringBuilder buf = new StringBuilder(noOfItems * 16);

                for (int i = startIndex; i < endIndex; ++i) {
                    if (i > startIndex) {
                        buf.append(separator);
                    }

                    if (array[i] != null) {
                        buf.append(array[i]);
                    }
                }

                return buf.toString();
            }
        }
    }

    public static String join(final Iterable<?> iterable, final String separator) {
        if (iterable == null) {
            return null;
        }
        return join(iterable.iterator(), separator);
    }

    public static String join(final Iterator<?> iterator, final String separator) {

        // handle null, zero and one elements before building a buffer
        if (iterator == null) {
            return null;
        }
        if (!iterator.hasNext()) {
            return "";
        }
        final Object first = iterator.next();
        if (!iterator.hasNext()) {
            return first == null ? "" : first.toString();
        }

        // two or more elements
        // Java default is 16, probably too small
        final StringBuilder buf = new StringBuilder(256);
        if (first != null) {
            buf.append(first);
        }

        while (iterator.hasNext()) {
            if (separator != null) {
                buf.append(separator);
            }
            final Object obj = iterator.next();
            if (obj != null) {
                buf.append(obj);
            }
        }
        return buf.toString();
    }


    /**
     * <p>Checks if a CharSequence is not empty (""), not null and not whitespace only.</p>
     *
     * <pre>
     * StringUtils.isNotBlank(null)      = false
     * StringUtils.isNotBlank("")        = false
     * StringUtils.isNotBlank(" ")       = false
     * StringUtils.isNotBlank("bob")     = true
     * StringUtils.isNotBlank("  bob  ") = true
     * </pre>
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is
     * not empty and not null and not whitespace
     * @since 2.0
     * @since 3.0 Changed signature from isNotBlank(String) to isNotBlank(CharSequence)
     */
    public static boolean isNotBlank(final CharSequence cs) {
        return !isBlank(cs);
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
     * 一般的类名称为： a.b.c.d.E
     *
     * @param className
     * @param packagesSet
     * @return
     */
    public static String isContainsPackage(String className, Set<String> packagesSet) {
        for (int i = className.length() - 1; i >= 0; i--) {
            if (className.charAt(i) == '.') {
                String p = className.substring(0, i);
                if (packagesSet.contains(p)) {
                    return p;
                }
            }
        }
        return null;
    }

    /**
     * 算法2简单粗暴，耗时比方法1多，但是无本质差距
     *
     * @param className
     * @param packagesSet
     * @return
     */
    public static String isContainsPackage2(String className, Set<String> packagesSet) {
        for (String p : packagesSet) {
            if (className.startsWith(p)) {
                return p;
            }
        }
        return null;
    }

    public static String array2String(byte[] a) {
        if (a == null) {
            return "";
        }

        int iMax = a.length - 1;
        if (iMax == -1) {
            return "";
        }

        StringBuilder b = new StringBuilder();
        for (int i = 0; ; i++) {
            b.append(a[i]);
            if (i == iMax) {
                return b.toString();
            }
            b.append(",");
        }
    }

    public static String array2String(String[] a) {
        if (a == null) {
            return "";
        }

        int iMax = a.length - 1;
        if (iMax == -1) {
            return "";
        }

        StringBuilder b = new StringBuilder();
        for (int i = 0; ; i++) {
            b.append(a[i]);
            if (i == iMax) {
                return b.toString();
            }
            b.append(",");
        }
    }

    public static String escape(String src) {
        try {
            char j;
            StringBuilder tmp = new StringBuilder();
            for (int i = 0; i < src.length(); i++) {
                j = src.charAt(i);
                if (j < 256) {
                    tmp.append(j);
                } else {
                    tmp.append("\\u");
                    tmp.append(Integer.toString(j, 16));
                }
            }
            return new String(tmp.toString().getBytes("UTF-8"));
        } catch (Exception ignored) {
            return "escape failed";
        }
    }
}
