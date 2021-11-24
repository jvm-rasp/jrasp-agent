package com.jrasp.api.event;

public class ThrowsEvent extends InvokeEvent {

    public final Throwable throwable;

    public ThrowsEvent(final int processId,
                       final int invokeId,
                       final Throwable throwable) {
        super(processId, invokeId, Type.THROWS);
        this.throwable = throwable;
    }

    ThrowsEvent(final Type type,
                final int processId,
                final int invokeId,
                final Throwable throwable) {
        super(processId, invokeId, type);
        this.throwable = throwable;
        // 对入参进行校验
        if (type != Type.THROWS
                && type != Type.IMMEDIATELY_THROWS) {
            throw new IllegalArgumentException(String.format("type must be %s or %s", Type.THROWS, Type.IMMEDIATELY_THROWS));
        }
    }

}
