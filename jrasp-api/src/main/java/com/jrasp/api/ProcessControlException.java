package com.jrasp.api;

public final class ProcessControlException extends Exception {

    // 流程控制状态
    private final State state;

    // 回应结果对象(直接返回或者抛出异常)
    private final Object respond;

    private final boolean isIgnoreProcessEvent;

    ProcessControlException(State state, Object respond) {
        this(false, state, respond);
    }

    ProcessControlException(boolean isIgnoreProcessEvent, State state, Object respond) {
        this.isIgnoreProcessEvent = isIgnoreProcessEvent;
        this.state = state;
        this.respond = respond;
    }

    public static void throwReturnImmediately(final Object object) throws ProcessControlException {
        throw new ProcessControlException(State.RETURN_IMMEDIATELY, object);
    }

    public static void throwThrowsImmediately(final Throwable throwable) throws ProcessControlException {
        throw new ProcessControlException(State.THROWS_IMMEDIATELY, throwable);
    }

    public boolean isIgnoreProcessEvent() {
        return isIgnoreProcessEvent;
    }

    public State getState() {
        return state;
    }

    public Object getRespond() {
        return respond;
    }

    @Override
    public Throwable fillInStackTrace() {
        return null;
    }


    /**
     * 流程控制状态
     */
    public enum State {

        /**
         * 立即返回
         */
        RETURN_IMMEDIATELY,

        /**
         * 立即抛出异常
         */
        THROWS_IMMEDIATELY,

        /**
         * 不干预任何流程
         *
         */
        NONE_IMMEDIATELY

    }

}
