package com.jrasp.api.event;

public class BeforeEvent extends InvokeEvent {

    public final ClassLoader javaClassLoader;

    public final String javaClassName;

    public final String javaMethodName;

    public final String javaMethodDesc;

    public final Object target;


    public final Object[] argumentArray;

    public BeforeEvent(final int processId,
                       final int invokeId,
                       final ClassLoader javaClassLoader,
                       final String javaClassName,
                       final String javaMethodName,
                       final String javaMethodDesc,
                       final Object target,
                       final Object[] argumentArray) {
        super(processId, invokeId, Type.BEFORE);
        this.javaClassLoader = javaClassLoader;
        this.javaClassName = javaClassName;
        this.javaMethodName = javaMethodName;
        this.javaMethodDesc = javaMethodDesc;
        this.target = target;
        this.argumentArray = argumentArray;
    }

    public BeforeEvent changeParameter(final int index,
                                       final Object changeValue) {
        argumentArray[index] = changeValue;
        return this;
    }

}
