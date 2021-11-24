package com.jrasp.core.util.matcher.structure;

import com.jrasp.api.util.LazyGet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.join;

public class BehaviorStructure extends MemberStructure {

    private final ClassStructure returnTypeClassStructure;
    private final List<ClassStructure> parameterTypeClassStructures;
    private final List<ClassStructure> exceptionTypeClassStructures;
    private final List<ClassStructure> annotationTypeClassStructures;

    BehaviorStructure(final Access access,
                      final String name,
                      final ClassStructure declaringClassStructure,
                      final ClassStructure returnTypeClassStructure,
                      final List<ClassStructure> parameterTypeClassStructures,
                      final List<ClassStructure> exceptionTypeClassStructures,
                      final List<ClassStructure> annotationTypeClassStructures) {
        super(access, name, declaringClassStructure);
        this.returnTypeClassStructure = returnTypeClassStructure;
        this.parameterTypeClassStructures = Collections.unmodifiableList(parameterTypeClassStructures);
        this.exceptionTypeClassStructures = Collections.unmodifiableList(exceptionTypeClassStructures);
        this.annotationTypeClassStructures = Collections.unmodifiableList(annotationTypeClassStructures);
    }

    public ClassStructure getReturnTypeClassStructure() {
        return returnTypeClassStructure;
    }

    public List<ClassStructure> getParameterTypeClassStructures() {
        return parameterTypeClassStructures;
    }

    public List<ClassStructure> getExceptionTypeClassStructures() {
        return exceptionTypeClassStructures;
    }

    public List<ClassStructure> getAnnotationTypeClassStructures() {
        return annotationTypeClassStructures;
    }

    private Collection<String> takeJavaClassNames(final Collection<ClassStructure> classStructures) {
        final Collection<String> javaClassNames = new ArrayList<String>();
        for (final ClassStructure classStructure : classStructures) {
            javaClassNames.add(classStructure.getJavaClassName());
        }
        return javaClassNames;
    }


    private final LazyGet<String> signCodeLazyGet = new LazyGet<String>() {
        @Override
        protected String initialValue() {
            return new StringBuilder(256)
                    .append(getDeclaringClassStructure().getJavaClassName())
                    .append("#")
                    .append(getName())
                    .append("(")
                    .append(join(takeJavaClassNames(getParameterTypeClassStructures()), ","))
                    .append(")")
                    .toString();
        }
    };

    public String getSignCode() {
        return signCodeLazyGet.get();
    }

    private final LazyGet<String> toStringLazyGet = new LazyGet<String>() {
        @Override
        protected String initialValue() {
            return new StringBuilder(256)
                    .append(getReturnTypeClassStructure().getJavaClassName())
                    .append(":[")
                    .append(join(takeJavaClassNames(getAnnotationTypeClassStructures()), ","))
                    .append("]:")
                    .append(getSignCode())
                    .append(":")
                    .append(join(takeJavaClassNames(getExceptionTypeClassStructures()), ","))
                    .toString();
        }
    };

    @Override
    public String toString() {
        return toStringLazyGet.get();
    }

    @Override
    public int hashCode() {
        return getSignCode().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof BehaviorStructure)
                && getSignCode().equals(((BehaviorStructure) obj).getSignCode());
    }
}
