package com.jrasp.api.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class BehaviorDescriptor {

    private final Type type;

    public BehaviorDescriptor(final Constructor<?> constructor) {
        this.type = Type.getType(constructor);
    }

    public BehaviorDescriptor(final Method method) {
        this.type = Type.getType(method);
    }

    public String getDescriptor() {
        return type.getDescriptor();
    }


    private static class Type {

        private static final int VOID = 0;

        private static final int BOOLEAN = 1;

        private static final int CHAR = 2;

        private static final int BYTE = 3;

        private static final int SHORT = 4;

        private static final int INT = 5;

        private static final int FLOAT = 6;

        private static final int LONG = 7;

        private static final int DOUBLE = 8;

        private static final int ARRAY = 9;

        private static final int OBJECT = 10;

        private static final int METHOD = 11;

        private static final Type VOID_TYPE = new Type(VOID, null, ('V' << 24)
                | (5 << 16) | (0 << 8) | 0, 1);

        private static final Type BOOLEAN_TYPE = new Type(BOOLEAN, null, ('Z' << 24)
                | (0 << 16) | (5 << 8) | 1, 1);

        private static final Type CHAR_TYPE = new Type(CHAR, null, ('C' << 24)
                | (0 << 16) | (6 << 8) | 1, 1);

        private static final Type BYTE_TYPE = new Type(BYTE, null, ('B' << 24)
                | (0 << 16) | (5 << 8) | 1, 1);

        private static final Type SHORT_TYPE = new Type(SHORT, null, ('S' << 24)
                | (0 << 16) | (7 << 8) | 1, 1);

        private static final Type INT_TYPE = new Type(INT, null, ('I' << 24)
                | (0 << 16) | (0 << 8) | 1, 1);

        private static final Type FLOAT_TYPE = new Type(FLOAT, null, ('F' << 24)
                | (2 << 16) | (2 << 8) | 1, 1);

        private static final Type LONG_TYPE = new Type(LONG, null, ('J' << 24)
                | (1 << 16) | (1 << 8) | 2, 1);

        private static final Type DOUBLE_TYPE = new Type(DOUBLE, null, ('D' << 24)
                | (3 << 16) | (3 << 8) | 2, 1);

        private final int sort;

        private final char[] buf;

        private final int off;

        private final int len;

        private Type(final int sort, final char[] buf, final int off, final int len) {
            this.sort = sort;
            this.buf = buf;
            this.off = off;
            this.len = len;
        }

        private static Type getType(final String typeDescriptor) {
            return getType(typeDescriptor.toCharArray(), 0);
        }

        public static Type getType(final Constructor<?> c) {
            return getType(getConstructorDescriptor(c));
        }

        public static Type getType(final Method m) {
            return getType(getMethodDescriptor(m));
        }

        private static Type getType(final char[] buf, final int off) {
            int len;
            switch (buf[off]) {
                case 'V':
                    return VOID_TYPE;
                case 'Z':
                    return BOOLEAN_TYPE;
                case 'C':
                    return CHAR_TYPE;
                case 'B':
                    return BYTE_TYPE;
                case 'S':
                    return SHORT_TYPE;
                case 'I':
                    return INT_TYPE;
                case 'F':
                    return FLOAT_TYPE;
                case 'J':
                    return LONG_TYPE;
                case 'D':
                    return DOUBLE_TYPE;
                case '[':
                    len = 1;
                    while (buf[off + len] == '[') {
                        ++len;
                    }
                    if (buf[off + len] == 'L') {
                        ++len;
                        while (buf[off + len] != ';') {
                            ++len;
                        }
                    }
                    return new Type(ARRAY, buf, off, len + 1);
                case 'L':
                    len = 1;
                    while (buf[off + len] != ';') {
                        ++len;
                    }
                    return new Type(OBJECT, buf, off + 1, len - 1);
                // case '(':
                default:
                    return new Type(METHOD, buf, off, buf.length - off);
            }
        }

        public String getDescriptor() {
            StringBuilder buf = new StringBuilder();
            getDescriptor(buf);
            return buf.toString();
        }

        private void getDescriptor(final StringBuilder buf) {
            if (this.buf == null) {
                // descriptor is in byte 3 of 'off' for primitive types (buf ==
                // null)
                buf.append((char) ((off & 0xFF000000) >>> 24));
            } else if (sort == OBJECT) {
                buf.append('L');
                buf.append(this.buf, off, len);
                buf.append(';');
            } else { // sort == ARRAY || sort == METHOD
                buf.append(this.buf, off, len);
            }
        }

        private static String getConstructorDescriptor(final Constructor<?> c) {
            Class<?>[] parameters = c.getParameterTypes();
            StringBuilder buf = new StringBuilder();
            buf.append('(');
            for (final Class<?> parameter : parameters) {
                getDescriptor(buf, parameter);
            }
            return buf.append(")V").toString();
        }

        private static String getMethodDescriptor(final Method m) {
            Class<?>[] parameters = m.getParameterTypes();
            StringBuilder buf = new StringBuilder();
            buf.append('(');
            for (final Class<?> parameter : parameters) {
                getDescriptor(buf, parameter);
            }
            buf.append(')');
            getDescriptor(buf, m.getReturnType());
            return buf.toString();
        }

        private static void getDescriptor(final StringBuilder buf, final Class<?> c) {
            Class<?> d = c;
            while (true) {
                if (d.isPrimitive()) {
                    char car;
                    if (d == Integer.TYPE) {
                        car = 'I';
                    } else if (d == Void.TYPE) {
                        car = 'V';
                    } else if (d == Boolean.TYPE) {
                        car = 'Z';
                    } else if (d == Byte.TYPE) {
                        car = 'B';
                    } else if (d == Character.TYPE) {
                        car = 'C';
                    } else if (d == Short.TYPE) {
                        car = 'S';
                    } else if (d == Double.TYPE) {
                        car = 'D';
                    } else if (d == Float.TYPE) {
                        car = 'F';
                    } else /* if (d == Long.TYPE) */ {
                        car = 'J';
                    }
                    buf.append(car);
                    return;
                } else if (d.isArray()) {
                    buf.append('[');
                    d = d.getComponentType();
                } else {
                    buf.append('L');
                    String name = d.getName();
                    int len = name.length();
                    for (int i = 0; i < len; ++i) {
                        char car = name.charAt(i);
                        buf.append(car == '.' ? '/' : car);
                    }
                    buf.append(';');
                    return;
                }
            }
        }

    }

}
