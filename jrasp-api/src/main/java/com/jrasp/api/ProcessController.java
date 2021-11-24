package com.jrasp.api;

import static com.jrasp.api.ProcessControlException.State.*;
import static com.jrasp.api.ProcessControlException.throwReturnImmediately;
import static com.jrasp.api.ProcessControlException.throwThrowsImmediately;

public final class ProcessController {

    public static void returnImmediately(final Object object) throws ProcessControlException {
        throwReturnImmediately(object);
    }

    public static void throwsImmediately(final Throwable throwable) throws ProcessControlException {
        throwThrowsImmediately(throwable);
    }

    public static void returnImmediatelyWithIgnoreProcessEvent(final Object object) throws ProcessControlException {
        throw new ProcessControlException(true, RETURN_IMMEDIATELY, object);
    }

    public static void throwsImmediatelyWithIgnoreProcessEvent(final Throwable throwable) throws ProcessControlException {
        throw new ProcessControlException(true, THROWS_IMMEDIATELY, throwable);
    }

    private static final ProcessControlException noneImmediatelyException
            = new ProcessControlException(NONE_IMMEDIATELY, null);

    private static final ProcessControlException noneImmediatelyWithIgnoreProcessEventException
            = new ProcessControlException(true, NONE_IMMEDIATELY, null);

    public static void noneImmediately() throws ProcessControlException {
        throw noneImmediatelyException;
    }

    public static void noneImmediatelyWithIgnoreProcessEvent() throws ProcessControlException {
        throw noneImmediatelyWithIgnoreProcessEventException;
    }

}
