package com.jrasp.agent.core.client;

import com.jrasp.agent.core.CoreConfigure;

import java.lang.instrument.Instrumentation;

public interface CoreClient {

    void init(CoreConfigure cfg, Instrumentation inst) throws Exception;

    boolean isInit();

    void close() throws Exception;

}
