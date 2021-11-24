package com.jrasp.core.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ObjectIDs {

    public static final int NULL_ID = 0;

    private final Sequencer objectIDSequencer = new Sequencer();

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    // 全局<对象:ID>映射表
    private final WeakHashMap<Object, Integer> objectIDMapping
            = new WeakHashMap<Object, Integer>();


    // --- ObjectID : Object 的映射关系维护 ----------------------------------------+
    private final ReferenceQueue<Object> rQueue = new ReferenceQueue<Object>(); //|
    private final HashMap<Integer, IdentityWeakReference> identityObjectMapping //|
            = new HashMap<Integer, IdentityWeakReference>();                    //|
    // ---------------------------------------------------------------------------+


    private ObjectIDs() {

    }

    public int identity(final Object object) {

        if (null == object) {
            return NULL_ID;
        }

        rwLock.readLock().lock();
        try {
            final Integer objectID = objectIDMapping.get(object);
            if (null != objectID) {
                return objectID;
            }
        } finally {
            rwLock.readLock().unlock();
            expungeIdentityObjectMapping();
        }

        rwLock.writeLock().lock();
        try {
            final Integer nextObjectID;
            if (objectIDMapping.containsKey(object)) {
                nextObjectID = objectIDMapping.get(object);
            } else {
                mapping(
                        nextObjectID = objectIDSequencer.next(),
                        object
                );
            }
            return nextObjectID;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private void mapping(final Integer objectID,
                         final Object object) {
        rwLock.writeLock().lock();
        try {
            // 映射 [object : objectID]
            objectIDMapping.put(object, objectID);

            // 映射 [objectID : object]
            identityObjectMapping.put(objectID, new IdentityWeakReference(objectID, object));
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * 清理失效的 [objectID : object] 映射
     */
    private void expungeIdentityObjectMapping() {
        for (Object x; (x = rQueue.poll()) != null; ) {
            synchronized (rQueue) {
                rwLock.writeLock().lock();
                try {
                    identityObjectMapping.remove(((IdentityWeakReference) x).objectID);
                } finally {
                    rwLock.writeLock().unlock();
                }
            }
        }
    }

    public <T> T getObject(final int objectID) {

        if (NULL_ID == objectID) {
            return null;
        }

        rwLock.readLock().lock();
        try {
            final Object object;
            final IdentityWeakReference reference = identityObjectMapping.get(objectID);
            if (null != reference
                    && null != (object = reference.get())) {
                return (T) object;
            } else {
                return null;
            }
        } finally {
            rwLock.readLock().unlock();
            expungeIdentityObjectMapping();
        }

    }

    // 带ObjectID标记的弱对象引用
    private class IdentityWeakReference extends WeakReference<Object> {

        // 对应的对象ID
        private final Integer objectID;

        private IdentityWeakReference(final Integer objectID,
                                      final Object referent) {
            super(referent, rQueue);
            this.objectID = objectID;
        }

    }

    public static final ObjectIDs instance = new ObjectIDs();

}
