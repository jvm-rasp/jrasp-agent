package com.jrasp.api;


public interface ModuleLifecycle extends LoadCompleted {

    void onLoad() throws Throwable;

    void onUnload() throws Throwable;

    void onActive() throws Throwable;

    void onFrozen() throws Throwable;

}
