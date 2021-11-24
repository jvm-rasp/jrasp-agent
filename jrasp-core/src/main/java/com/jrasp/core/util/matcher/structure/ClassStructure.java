package com.jrasp.core.util.matcher.structure;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


public interface ClassStructure {

    String getJavaClassName();

    ClassLoader getClassLoader();

    ClassStructure getSuperClassStructure();

    List<ClassStructure> getInterfaceClassStructures();

    LinkedHashSet<ClassStructure> getFamilySuperClassStructures();

    Set<ClassStructure> getFamilyInterfaceClassStructures();

    Set<ClassStructure> getFamilyTypeClassStructures();

    List<ClassStructure> getAnnotationTypeClassStructures();

    Set<ClassStructure> getFamilyAnnotationTypeClassStructures();

    List<BehaviorStructure> getBehaviorStructures();

    Access getAccess();

}
