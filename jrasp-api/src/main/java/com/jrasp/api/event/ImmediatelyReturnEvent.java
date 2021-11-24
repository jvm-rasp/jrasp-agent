package com.jrasp.api.event;

public class ImmediatelyReturnEvent extends ReturnEvent {

    public ImmediatelyReturnEvent(final int processId,
                                  final int invokeId,
                                  final Object object) {
        super(Type.IMMEDIATELY_RETURN, processId, invokeId, object);
    }
}
