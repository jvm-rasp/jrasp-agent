package com.jrasp.api.resource;

import com.jrasp.api.event.Event;

@Deprecated
public interface EventMonitor {

    interface EventPoolInfo {

        int getNumActive();

        int getNumActive(Event.Type type);

        int getNumIdle();

        int getNumIdle(Event.Type type);

    }

    EventPoolInfo getEventPoolInfo();

}
