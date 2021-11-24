package com.jrasp.api.util;


public class GaStringUtils {

    public static String getJavaClassName(final Class<?> clazz) {
        return clazz.isArray()
                ? clazz.getCanonicalName()
                : clazz.getName();
    }

    public static String[] getJavaClassNameArray(final Class<?>[] classArray) {
        if (GaArrayUtils.isEmpty(classArray)) {
            return null;
        }
        final String[] javaClassNameArray = new String[classArray.length];
        for (int index = 0; index < classArray.length; index++) {
            javaClassNameArray[index] = getJavaClassName(classArray[index]);
        }
        return javaClassNameArray;
    }

    public static boolean isEmpty(String string) {
        return null == string
                || string.isEmpty();
    }

    public static boolean matching(final String string, final String wildcard) {
        return null != wildcard
                && null != string
                && matching(string, wildcard, 0, 0);
    }

    private static boolean matching(String string, String wildcard, int stringStartNdx, int patternStartNdx) {
        int pNdx = patternStartNdx;
        int sNdx = stringStartNdx;
        int pLen = wildcard.length();
        if (pLen == 1) {
            if (wildcard.charAt(0) == '*') {     // speed-up
                return true;
            }
        }
        int sLen = string.length();
        boolean nextIsNotWildcard = false;

        while (true) {

            // check if end of string and/or pattern occurred
            if ((sNdx >= sLen)) {   // end of string still may have pending '*' callback pattern
                while ((pNdx < pLen) && (wildcard.charAt(pNdx) == '*')) {
                    pNdx++;
                }
                return pNdx >= pLen;
            }
            if (pNdx >= pLen) {         // end of pattern, but not end of the string
                return false;
            }
            char p = wildcard.charAt(pNdx);    // pattern char

            // perform logic
            if (!nextIsNotWildcard) {

                if (p == '\\') {
                    pNdx++;
                    nextIsNotWildcard = true;
                    continue;
                }
                if (p == '?') {
                    sNdx++;
                    pNdx++;
                    continue;
                }
                if (p == '*') {
                    char pnext = 0;           // next pattern char
                    if (pNdx + 1 < pLen) {
                        pnext = wildcard.charAt(pNdx + 1);
                    }
                    if (pnext == '*') {         // double '*' have the same effect as one '*'
                        pNdx++;
                        continue;
                    }
                    int i;
                    pNdx++;

                    // find recursively if there is any substring from the end of the
                    // line that matches the rest of the pattern !!!
                    for (i = string.length(); i >= sNdx; i--) {
                        if (matching(string, wildcard, i, pNdx)) {
                            return true;
                        }
                    }
                    return false;
                }
            } else {
                nextIsNotWildcard = false;
            }

            // check if pattern char and string char are equals
            if (p != string.charAt(sNdx)) {
                return false;
            }

            // everything matches for now, continue
            sNdx++;
            pNdx++;
        }
    }

}
