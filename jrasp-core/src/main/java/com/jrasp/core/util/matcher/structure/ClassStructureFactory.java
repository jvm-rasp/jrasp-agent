package com.jrasp.core.util.matcher.structure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class ClassStructureFactory {

    private static final Logger logger = LoggerFactory.getLogger(ClassStructureFactory.class);

    public static ClassStructure createClassStructure(final Class<?> clazz) {
        return new ClassStructureImplByJDK(clazz);
    }

    public static ClassStructure createClassStructure(final InputStream classInputStream,
                                                      final ClassLoader loader) {
        try {
            return new ClassStructureImplByAsm(classInputStream, loader);
        } catch (IOException cause) {
            logger.warn("create class structure failed by using ASM, return null. loader={};", loader, cause);
            return null;
        }
    }

    public static ClassStructure createClassStructure(final byte[] classByteArray,
                                                      final ClassLoader loader) {
        return new ClassStructureImplByAsm(classByteArray, loader);
    }

}
