package com.jrasp.agent.encrypt.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class StrUtils {

    public static List<String> toList(String strs) {
        List<String> list = new ArrayList<>();
        if (strs != null && strs.length() > 0) {
            String[] ss = strs.split(",");
            for (String s : ss) {
                if (s.trim().length() > 0) {
                    list.add(s.trim());
                }
            }
        }
        return list;
    }

    public static boolean isMatch(String match, String testString) {
        String regex = match.replaceAll("\\?", "(.?)")
                .replaceAll("\\*+", "(.*?)");
        return Pattern.matches(regex, testString);
    }

    public static boolean isMatchs(List<String> matches, String testString, boolean dv) {
        if (matches == null || matches.size() == 0) {
            return dv;
        }

        for (String m : matches) {
            if (StrUtils.isMatch(m, testString) || testString.startsWith(m) || testString.endsWith(m)) {
                return true;
            }
        }
        return false;
    }

}
