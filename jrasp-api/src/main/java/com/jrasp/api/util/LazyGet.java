package com.jrasp.api.util;

public abstract class LazyGet<T> {

    private volatile boolean isInit = false;
    private volatile T object;

    abstract protected T initialValue() throws Throwable;

    public T get() {

        if (isInit) {
            return object;
        }

        // lazy get
        try {
            object = initialValue();
            isInit = true;
            return object;
        } catch (Throwable throwable) {
            throw new LazyGetUnCaughtException(throwable);
        }

    }

    private static class LazyGetUnCaughtException extends RuntimeException {
        LazyGetUnCaughtException(Throwable cause) {
            super(cause);
        }
    }

}
