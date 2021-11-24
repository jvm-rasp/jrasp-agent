package com.jrasp.api.event;

public class CallReturnEvent extends InvokeEvent {
    public CallReturnEvent(int processId, int invokeId) {
        super(processId, invokeId, Type.CALL_RETURN);
    }
}
