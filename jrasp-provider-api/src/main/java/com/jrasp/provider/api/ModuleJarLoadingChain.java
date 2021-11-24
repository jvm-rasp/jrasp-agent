package com.jrasp.provider.api;

import java.io.File;

public interface ModuleJarLoadingChain {
    void loading(File moduleJarFile) throws Throwable;
}
