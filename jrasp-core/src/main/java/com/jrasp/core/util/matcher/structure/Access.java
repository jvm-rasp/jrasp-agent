package com.jrasp.core.util.matcher.structure;

public interface Access {

    boolean isPublic();

    boolean isPrivate();

    boolean isProtected();

    boolean isStatic();

    boolean isFinal();

    boolean isInterface();

    boolean isNative();

    boolean isAbstract();

    boolean isEnum();

    boolean isAnnotation();

}
