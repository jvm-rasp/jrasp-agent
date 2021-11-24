package com.jrasp.api.event;

public class CallBeforeEvent extends InvokeEvent {

    public final int lineNumber;

    public final String owner;

    public final String name;

    public final String desc;

    public CallBeforeEvent(final int processId,
                           final int invokeId,
                           final int lineNumber,
                           final String owner,
                           final String name,
                           final String desc) {
        super(processId, invokeId, Type.CALL_BEFORE);
        this.lineNumber = lineNumber;
        this.owner = owner;
        this.name = name;
        this.desc = desc;
    }

}
