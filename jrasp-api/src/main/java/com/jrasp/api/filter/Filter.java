package com.jrasp.api.filter;

public interface Filter {

    boolean doClassFilter(int access,
                          String javaClassName,
                          String superClassTypeJavaClassName,
                          String[] interfaceTypeJavaClassNameArray,
                          String[] annotationTypeJavaClassNameArray);

    boolean doMethodFilter(int access,
                           String javaMethodName,
                           String[] parameterTypeJavaClassNameArray,
                           String[] throwsTypeJavaClassNameArray,
                           String[] annotationTypeJavaClassNameArray);

}
