package com.jrasp.api.util;

import java.util.HashMap;
import java.util.Map;

public abstract class CacheGet<K, V> {

    private final Map<K, V> cache = new HashMap<K, V>();

    public V getFromCache(K key) {
        if (!cache.containsKey(key)) {
            try {
                final V value;
                cache.put(key, value = load(key));
                return value;
            } catch (Throwable cause) {
                throw new CacheLoadUnCaughtException(cause);
            }
        } else {
            return cache.get(key);
        }
    }

    protected abstract V load(K key) throws Throwable;

    private final static class CacheLoadUnCaughtException extends RuntimeException {
        CacheLoadUnCaughtException(Throwable cause) {
            super(cause);
        }
    }

}
