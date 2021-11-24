package com.jrasp.api.util;

import java.util.Collection;

public class GaCollectionUtils {

    public static <E> E add(Collection<E> collection, E e) {
        collection.add(e);
        return e;
    }

}
