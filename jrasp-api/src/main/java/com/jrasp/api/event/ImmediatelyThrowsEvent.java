package com.jrasp.api.event;

public class ImmediatelyThrowsEvent extends ThrowsEvent {

    public ImmediatelyThrowsEvent(final int processId,
                                  final int invokeId,
                                  final Throwable throwable) {
        super(Type.IMMEDIATELY_THROWS, processId, invokeId, throwable);
    }

}
