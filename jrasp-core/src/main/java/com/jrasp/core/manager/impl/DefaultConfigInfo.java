package com.jrasp.core.manager.impl;

import com.jrasp.api.ConfigInfo;
import com.jrasp.api.Information;
import com.jrasp.core.CoreConfigure;
import com.jrasp.core.server.ProxyCoreServer;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;

class DefaultConfigInfo implements ConfigInfo {

    private final CoreConfigure cfg;

    public DefaultConfigInfo(CoreConfigure cfg) {
        this.cfg = cfg;
    }

    @Override
    public String getNamespace() {
        return cfg.getNamespace();
    }

    @Override
    public Information.Mode getMode() {
        return cfg.getLaunchMode();
    }

    @Override
    public boolean isEnableUnsafe() {
        return cfg.isEnableUnsafe();
    }

    @Override
    public String getRaspHome() {
        return cfg.getRaspHome();
    }

    @Override
    public String getModuleLibPath() {
        return getSystemModuleLibPath();
    }

    @Override
    public String getSystemModuleLibPath() {
        return cfg.getSystemModuleLibPath();
    }

    @Override
    public String getSystemProviderLibPath() {
        return cfg.getProviderLibPath();
    }

    @Override
    public String getUserModuleLibPath() {
        return cfg.getUserModuleLibPath();
    }

    @Override
    public InetSocketAddress getServerAddress() {
        try {
            return ProxyCoreServer.getInstance().getLocal();
        } catch (Throwable cause) {
            return new InetSocketAddress("0.0.0.0", 0);
        }
    }

    @Override
    public String getServerCharset() {
        return cfg.getServerCharset().name();
    }

    @Override
    public String getVersion() {
        final InputStream is = getClass().getResourceAsStream("/version");
        try {
            return IOUtils.toString(is);
        } catch (IOException e) {
            // impossible
            return "UNKNOW_VERSION";
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    @Override
    public String getEncryptionkey() {
        return cfg.getEncryption();
    }

    @Override
    public String getUsername() {
        return cfg.getUsername();
    }

    @Override
    public String getPassword() {
        return cfg.getPassword();
    }

    @Override
    public void setEncryptionkey(String encryptionkey) {
        cfg.setEncryption(encryptionkey);
    }

    @Override
    public void setUsername(String username) {
        cfg.setUsername(username);
    }

    @Override
    public void setPassword(String password) {
        cfg.setPassword(password);
    }
}
