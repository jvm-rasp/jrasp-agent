package com.jrasp.api.filter;

import com.jrasp.api.annotation.IncludeBootstrap;
import com.jrasp.api.annotation.IncludeSubClasses;

public interface ExtFilter extends Filter {

    boolean isIncludeSubClasses();

    boolean isIncludeBootstrap();

    class ExtFilterFactory {

        public static ExtFilter make(final Filter filter,
                                     final boolean isIncludeSubClasses,
                                     final boolean isIncludeBootstrap) {
            return new ExtFilter() {

                @Override
                public boolean isIncludeSubClasses() {
                    return isIncludeSubClasses;
                }

                @Override
                public boolean isIncludeBootstrap() {
                    return isIncludeBootstrap;
                }

                @Override
                public boolean doClassFilter(final int access,
                                             final String javaClassName,
                                             final String superClassTypeJavaClassName,
                                             final String[] interfaceTypeJavaClassNameArray,
                                             final String[] annotationTypeJavaClassNameArray) {
                    return filter.doClassFilter(
                            access,
                            javaClassName,
                            superClassTypeJavaClassName,
                            interfaceTypeJavaClassNameArray,
                            annotationTypeJavaClassNameArray
                    );
                }

                @Override
                public boolean doMethodFilter(final int access,
                                              final String javaMethodName,
                                              final String[] parameterTypeJavaClassNameArray,
                                              final String[] throwsTypeJavaClassNameArray,
                                              final String[] annotationTypeJavaClassNameArray) {
                    return filter.doMethodFilter(
                            access,
                            javaMethodName,
                            parameterTypeJavaClassNameArray,
                            throwsTypeJavaClassNameArray,
                            annotationTypeJavaClassNameArray
                    );
                }
            };
        }

        public static ExtFilter make(final Filter filter) {
            return
                    filter instanceof ExtFilter
                            ? (ExtFilter) filter
                            : make(
                            filter,
                            filter.getClass().isAnnotationPresent(IncludeSubClasses.class),
                            filter.getClass().isAnnotationPresent(IncludeBootstrap.class)
                    );
        }

    }

}
