package com.jrasp.api.resource;

import com.jrasp.api.event.Event;
import com.jrasp.api.filter.Filter;
import com.jrasp.api.listener.EventListener;
import com.jrasp.api.listener.ext.EventWatchCondition;


public interface ModuleEventWatcher {

    int watch(Filter filter, EventListener listener, Progress progress, Event.Type... eventType);

    int watch(Filter filter, EventListener listener, Event.Type... eventType);

    int watch(EventWatchCondition condition, EventListener listener, Progress progress, Event.Type... eventType);

    void delete(int watcherId, Progress progress);

    void delete(int watcherId);

    void watching(Filter filter,
                  EventListener listener,
                  Progress wProgress,
                  WatchCallback watchCb,
                  Progress dProgress,
                  Event.Type... eventType
    ) throws Throwable;

    void watching(Filter filter,
                  EventListener listener,
                  WatchCallback watchCb,
                  Event.Type... eventType
    ) throws Throwable;


    interface WatchCallback {

        void watchCompleted() throws Throwable;

    }


    interface Progress {

        void begin(int total);

        void progressOnSuccess(Class<?> clazz, int index);

        void progressOnFailed(Class<?> clazz, int index, Throwable cause);

        void finish(int cCnt, int mCnt);

    }

}
