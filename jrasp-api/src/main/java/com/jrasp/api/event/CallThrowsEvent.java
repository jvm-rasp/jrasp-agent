package com.jrasp.api.event;

public class CallThrowsEvent extends InvokeEvent {

    public final String throwException;

    public CallThrowsEvent(int processId, int invokeId, String throwException) {
        super(processId, invokeId, Type.CALL_THROWS);
        this.throwException = throwException;
    }

}
