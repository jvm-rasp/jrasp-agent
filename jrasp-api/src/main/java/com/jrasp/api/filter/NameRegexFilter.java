package com.jrasp.api.filter;

public class NameRegexFilter implements Filter {

    // 类名正则表达式
    private final String javaNameRegex;

    // 方法名正则表达式
    private final String javaMethodRegex;

    public NameRegexFilter(String javaNameRegex, String javaMethodRegex) {
        this.javaNameRegex = javaNameRegex;
        this.javaMethodRegex = javaMethodRegex;
    }

    @Override
    public boolean doClassFilter(final int access,
                                 final String javaClassName,
                                 final String superClassTypeJavaClassName,
                                 final String[] interfaceTypeJavaClassNameArray,
                                 final String[] annotationTypeJavaClassNameArray) {
        return javaClassName.matches(javaNameRegex);
    }

    @Override
    public boolean doMethodFilter(final int access,
                                  final String javaMethodName,
                                  final String[] parameterTypeJavaClassNameArray,
                                  final String[] throwsTypeJavaClassNameArray,
                                  final String[] annotationTypeJavaClassNameArray) {
        return javaMethodName.matches(javaMethodRegex);
    }

}
