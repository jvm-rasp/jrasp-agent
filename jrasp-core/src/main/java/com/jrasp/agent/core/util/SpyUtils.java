package com.jrasp.agent.core.util;

import com.jrasp.agent.core.enhance.weaver.EventListenerHandler;

import java.com.jrasp.agent.bridge110.Spy;

/**
 * Spy类操作工具类
 *
 * @author luajia@taobao.com
 */
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
