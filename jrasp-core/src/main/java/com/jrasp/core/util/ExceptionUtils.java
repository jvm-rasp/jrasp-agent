package com.jrasp.core.util;

import java.io.IOException;

public class ExceptionUtils {

    public static boolean isCauseByIOException(final Throwable cause) {
        return isCauseByTargetException(cause, IOException.class);
    }

    public static boolean isCauseByTargetException(final Throwable cause, final Class<? extends Throwable> targetCauseType) {
        if (targetCauseType.isAssignableFrom(cause.getClass())) {
            return true;
        }
        return null != cause.getCause() && isCauseByTargetException(cause.getCause(), targetCauseType);
    }

}
