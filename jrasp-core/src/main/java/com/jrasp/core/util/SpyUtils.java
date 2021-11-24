package com.jrasp.core.util;

import com.jrasp.core.enhance.weaver.EventListenerHandler;

import java.com.jrasp.spy.Spy;

public class SpyUtils {

    /**
     * 初始化Spy类
     *
     * @param namespace 命名空间
     */
    public synchronized static void init(final String namespace) {

        if (!Spy.isInit(namespace)) {
            Spy.init(namespace, EventListenerHandler.getSingleton());
        }

    }

    /**
     * 清理Spy中的命名空间
     *
     * @param namespace 命名空间
     */
    public synchronized static void clean(final String namespace) {
        Spy.clean(namespace);
    }

}
