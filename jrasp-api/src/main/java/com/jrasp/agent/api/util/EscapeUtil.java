package com.jrasp.agent.api.util;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * from hutools
 * 处理json中的字符串
 */
public class EscapeUtil {

    private static final char SPACE = ' ';

    private static final char[] DIGITS_LOWER = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static String quote(String string) {
        StringWriter sw = new StringWriter();
        try {
            return quote(string, sw).toString();
        } catch (IOException ignored) {
            // will never happen - we are writing to a string writer
            return "";
        }
    }

    private static Writer quote(String str, Writer writer) throws IOException {
        if (str == null || str.length() == 0) {
            writer.write("\"\"");
            return writer;
        }

        char b; // 前一个字符
        char c = 0; // 当前字符
        int len = str.length();
        writer.write('"');
        for (int i = 0; i < len; i++) {
            b = c;
            c = str.charAt(i);
            switch (c) {
                case '\\':
                case '"':
                    writer.write("\\");
                    writer.write(c);
                    break;
                case '/':
                    if (b == '<') {
                        writer.write('\\');
                    }
                    writer.write(c);
                    break;
                default:
                    writer.write(escape(c));
            }
        }
        writer.write('"');
        return writer;
    }

    /**
     * 转义不可见字符<br>
     * 见：https://en.wikibooks.org/wiki/Unicode/Character_reference/0000-0FFF
     *
     * @param c 字符
     * @return 转义后的字符串
     */
    private static String escape(char c) {
        switch (c) {
            case '\b':
                return "\\b";
            case '\t':
                return "\\t";
            case '\n':
                return "\\n";
            case '\f':
                return "\\f";
            case '\r':
                return "\\r";
            default:
                if (c < SPACE || //
                        (c >= '\u0080' && c <= '\u00a0') || //
                        (c >= '\u2000' && c <= '\u2010') || //
                        (c >= '\u2028' && c <= '\u202F') || //
                        (c >= '\u2066' && c <= '\u206F')//
                ) {
                    return toUnicodeHex(c);
                } else {
                    return Character.toString(c);
                }
        }
    }

    private static String toUnicodeHex(char ch) {
        StringBuilder sb = new StringBuilder(6);
        sb.append("\\u");
        sb.append(DIGITS_LOWER[(ch >> 12) & 15]);
        sb.append(DIGITS_LOWER[(ch >> 8) & 15]);
        sb.append(DIGITS_LOWER[(ch >> 4) & 15]);
        sb.append(DIGITS_LOWER[(ch) & 15]);
        return sb.toString();
    }
}
