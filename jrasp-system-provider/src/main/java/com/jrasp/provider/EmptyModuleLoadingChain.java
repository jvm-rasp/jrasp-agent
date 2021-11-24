package com.jrasp.provider;

import com.jrasp.api.Module;
import com.jrasp.provider.api.ModuleLoadingChain;

import java.io.File;

public class EmptyModuleLoadingChain implements ModuleLoadingChain {

    @Override
    public void loading(final String uniqueId,
                        final Class moduleClass,
                        final Module module,
                        final File moduleJarFile,
                        final ClassLoader moduleClassLoader) throws Throwable {
    }
}
