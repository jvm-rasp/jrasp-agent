package com.jrasp.provider.api;

import com.jrasp.api.Module;

import java.io.File;

public interface ModuleLoadingChain {
    void loading(final String uniqueId, final Class moduleClass, final Module module, final File moduleJarFile,
                 final ClassLoader moduleClassLoader) throws Throwable;
}
