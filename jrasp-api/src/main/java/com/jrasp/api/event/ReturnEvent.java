package com.jrasp.api.event;

public class ReturnEvent extends InvokeEvent {

    public final Object object;

    public ReturnEvent(final int processId,
                       final int invokeId,
                       final Object object) {
        super(processId, invokeId, Type.RETURN);
        this.object = object;
    }

    ReturnEvent(final Type type,
                final int processId,
                final int invokeId,
                final Object object) {
        super(processId, invokeId, type);
        this.object = object;

        // 对入参进行校验
        if (type != Type.IMMEDIATELY_RETURN
                && type != Type.RETURN) {
            throw new IllegalArgumentException(String.format("type must be %s or %s", Type.RETURN, Type.IMMEDIATELY_RETURN));
        }

    }

}
