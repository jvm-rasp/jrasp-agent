package com.jrasp.core.util;

import org.apache.commons.lang3.ArrayUtils;

public class BitUtils {

    public static boolean isIn(int target, int... maskArray) {
        if (ArrayUtils.isEmpty(maskArray)) {
            return false;
        }
        for (int mask : maskArray) {
            if ((target & mask) == mask) {
                return true;
            }
        }
        return false;
    }

}
