package com.jrasp.api.resource;

import com.jrasp.api.filter.Filter;

import java.util.Iterator;
import java.util.Set;


public interface LoadedClassDataSource {

    Set<Class<?>> list();

    Set<Class<?>> find(Filter filter);

    Iterator<Class<?>> iteratorForLoadedClasses();

}
