package com.jrasp.api.event;

public abstract class InvokeEvent extends Event {

    public final int processId;

    public final int invokeId;

    protected InvokeEvent(int processId, int invokeId, Type type) {
        super(type);
        this.processId = processId;
        this.invokeId = invokeId;
    }

}
