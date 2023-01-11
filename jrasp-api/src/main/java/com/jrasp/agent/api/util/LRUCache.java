package com.jrasp.agent.api.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * from open-rasp
 */
public class LRUCache<K, V> {

    private static final float HASH_TABLE_LOAD_FACTOR = 0.75f;

    private LinkedHashMap<K, V> map;
    private int cacheSize;
    private final Lock lock = new ReentrantLock();

    /**
     * Creates a new LRU cache.
     *
     * @param cacheSize the maximum number of entries that will be kept in this cache.
     */
    public LRUCache(int cacheSize) {
        this.cacheSize = cacheSize;
        int hashTableCapacity = (int) Math.ceil(cacheSize / HASH_TABLE_LOAD_FACTOR) + 1;
        map = new LinkedHashMap<K, V>(hashTableCapacity, HASH_TABLE_LOAD_FACTOR, true) {

            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > LRUCache.this.cacheSize;
            }
        };
    }

    /**
     * get an entry from the cache.
     *
     * @param key the key whose associated value is to be returned.
     * @return the value associated to this key, or null if no value with this key exists in the cache.
     */
    public V get(K key) {

        try {
            lock.lock();
            return map.get(key);
        } finally {
            lock.unlock();
        }

    }

    /**
     * Add an entry to this cache.
     *
     * @param key   the key with which the specified value is to be associated.
     * @param value a value to be associated with the specified key.
     */
    public void put(K key, V value) {
        try {
            lock.lock();
            map.put(key, value);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Clear the cache.
     */
    public void clear() {

        try {
            lock.lock();
            map.clear();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Return the existence of  in the cache.
     *
     * @param key the key with which the specified value is to be associated.
     */
    public boolean isContainsKey(K key) {

        try {
            lock.lock();
            return map.containsKey(key);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Return the Set of keys in the cache.
     */
    public Set<K> getKeySet() {

        try {
            lock.lock();
            return map.keySet();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Return the Set of Entries in the cache.
     */
    public Set<Map.Entry<K, V>> getEntrySet() {
        try {
            lock.lock();
            return map.entrySet();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Return the real size of the cache.
     */
    public int realSize() {
        try {
            lock.lock();
            return map.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Return the max size of the cache.
     */
    public int maxSize() {
        try {
            lock.lock();
            return this.cacheSize;
        } finally {
            lock.unlock();
        }
    }

    /**
     * remove element from the cache.
     */
    public void remove(K key) {
        try {
            lock.lock();
            map.remove(key);
        } finally {
            lock.unlock();
        }
    }


}