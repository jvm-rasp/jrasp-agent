package com.jrasp.agent.module.sql.algorithm.impl.checker;

import org.apache.commons.lang3.StringEscapeUtils;

import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;

public class CheckerUtils {
    public static String tryDecodeString(String src) {
        if (src == null) {
            return null;
        }
        boolean isBase64 = true;
        int length = src.length();
        for (int i = 0; i < length; i++) {
            char c = src.charAt(i);
            if (('A' > c || 'Z' < c) && ('a' > c || 'z' < c) && ('0' > c || '9' < c) && c != '+' && c != '/' && (c != '=' || i < length - 2)) {
                isBase64 = false;
                break;
            }
        }
        if (isBase64) {
            try {
                return new String(DatatypeConverter.parseBase64Binary(src));
            } catch (Throwable ignored) {
            }
        }
        if (src.contains("%")) {
            try {
                return decode(src, "UTF-8");
            } catch (Throwable ignored) {
            }
        }
        if (src.contains("\\u")) {
            try {
                return StringEscapeUtils.unescapeJava(src);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    public static String decode(String s, String enc) throws UnsupportedEncodingException {
        boolean needToChange = false;
        int numChars = s.length();
        StringBuilder sb = new StringBuilder((numChars > 500) ? (numChars / 2) : numChars);
        int i = 0;
        if (enc.isEmpty()) {
            throw new UnsupportedEncodingException("URLDecoder: empty string enc parameter");
        }
        byte[] bytes = null;
        while (i < numChars) {
            int pos;
            char c = s.charAt(i);
            switch (c) {
                case '+':
                    sb.append(' ');
                    i++;
                    needToChange = true;
                    continue;
                case '%':
                    pos = 0;
                    try {
                        if (bytes == null)
                            bytes = new byte[numChars - i];
                        while (i + 2 < numChars && c == '%') {
                            int v = Integer.parseInt(s.substring(i + 1, i + 3), 16);
                            if (v < 0) {
                                bytes[pos++] = 37;
                                bytes[pos++] = (byte) s.charAt(i + 1);
                                bytes[pos++] = (byte) s.charAt(i + 2);
                                i += 3;
                                if (i < numChars)
                                    c = s.charAt(i);
                                continue;
                            }
                            bytes[pos++] = (byte) v;
                            needToChange = true;
                            i += 3;
                            if (i < numChars)
                                c = s.charAt(i);
                        }
                        if (i < numChars && c == '%') {
                            bytes[pos++] = 37;
                            i++;
                            if (i < numChars) {
                                bytes[pos++] = (byte) s.charAt(i + 1);
                                i++;
                            }
                        }
                        sb.append(new String(bytes, 0, pos, enc));
                    } catch (NumberFormatException e) {
                        if (pos != 0)
                            sb.append(new String(bytes, 0, pos, enc));
                        if (s.charAt(i + 1) == '%') {
                            sb.append(s, i, i + 1);
                            i++;
                            continue;
                        }
                        if (s.charAt(i + 2) == '%') {
                            sb.append(s, i, i + 2);
                            i += 2;
                            continue;
                        }
                        if (s.charAt(i + 1) == 'u' && i + 5 < numChars) {
                            try {
                                bytes[0] = (byte) Integer.parseInt(s.substring(i + 2, i + 4), 16);
                                bytes[1] = (byte) Integer.parseInt(s.substring(i + 4, i + 6), 16);
                                sb.append(new String(bytes, 0, 2, "unicode"));
                                needToChange = true;
                                i += 6;
                            } catch (NumberFormatException ex) {
                                sb.append(s, i, i + 3);
                                i += 3;
                            }
                            continue;
                        }
                        sb.append(s, i, i + 3);
                        i += 3;
                    }
                    continue;
            }
            sb.append(c);
            i++;
        }
        return needToChange ? sb.toString() : s;
    }
}
