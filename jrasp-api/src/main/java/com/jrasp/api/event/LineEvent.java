package com.jrasp.api.event;

import static com.jrasp.api.event.Event.Type.LINE;

public class LineEvent extends InvokeEvent {

    public final int lineNumber;

    public LineEvent(int processId, int invokeId, int lineNumber) {
        super(processId, invokeId, LINE);
        this.lineNumber = lineNumber;
    }

}
