package com.jrasp.core.classloader;

import com.jrasp.api.annotation.Stealth;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * 服务提供库ClassLoader
 */
@Stealth
public class ProviderClassLoader extends RoutingURLClassLoader {

    public ProviderClassLoader(final File providerJarFile,
                               final ClassLoader raspClassLoader) throws IOException {
        super(
                new URL[]{new URL("file:" + providerJarFile.getPath())},
                new Routing(
                        raspClassLoader,
                        "^com\\.jrasp\\.api\\..*",
                        "^com\\.jrasp\\.provider\\..*"
                )
        );
    }
}
