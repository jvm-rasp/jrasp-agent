package com.jrasp.api.listener;

import com.jrasp.api.event.Event;

public interface EventListener {
    void onEvent(Event event) throws Throwable;
}
