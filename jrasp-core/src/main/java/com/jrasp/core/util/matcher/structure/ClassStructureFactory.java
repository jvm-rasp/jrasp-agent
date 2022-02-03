package com.jrasp.core.util.matcher.structure;


import com.jrasp.api.log.Log;
import com.jrasp.core.log.LogFactory;

import java.io.IOException;
import java.io.InputStream;

import static com.jrasp.core.log.AgentLogIdConstant.AGENT_COMMON_LOG_ID;

public class ClassStructureFactory {

    private final static Log logger = LogFactory.getLog(ClassStructureFactory.class);

    public static ClassStructure createClassStructure(final Class<?> clazz) {
        return new ClassStructureImplByJDK(clazz);
    }

    public static ClassStructure createClassStructure(final InputStream classInputStream,
                                                      final ClassLoader loader) {
        try {
            return new ClassStructureImplByAsm(classInputStream, loader);
        } catch (IOException cause) {
            logger.warn(AGENT_COMMON_LOG_ID,"create class structure failed by using ASM, return null. loader={};", loader, cause);
            return null;
        }
    }

    public static ClassStructure createClassStructure(final byte[] classByteArray,
                                                      final ClassLoader loader) {
        return new ClassStructureImplByAsm(classByteArray, loader);
    }

}
