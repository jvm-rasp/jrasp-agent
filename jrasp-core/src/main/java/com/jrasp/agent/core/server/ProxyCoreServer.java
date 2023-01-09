package com.jrasp.agent.core.server;

import com.jrasp.agent.core.CoreConfigure;
import com.jrasp.agent.core.server.socket.SocketServer;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.InetSocketAddress;

public class ProxyCoreServer implements CoreServer {

    private final static Class<? extends CoreServer> classOfCoreServerImpl = SocketServer.class;

    private final CoreServer proxy;

    private ProxyCoreServer(CoreServer proxy) {
        this.proxy = proxy;
    }

    public static CoreServer getInstance() {
        try {
            return new ProxyCoreServer((CoreServer) classOfCoreServerImpl.getMethod("getInstance").invoke(null));
        } catch (Throwable cause) {
            throw new RuntimeException(cause);
        }
    }

    @Override
    public boolean isBind() {
        return proxy.isBind();
    }

    @Override
    public void unbind() throws IOException {
        proxy.unbind();
    }

    @Override
    public InetSocketAddress getLocal() throws IOException {
        return proxy.getLocal();
    }

    @Override
    public void bind(CoreConfigure cfg, Instrumentation inst) throws IOException {
        proxy.bind(cfg, inst);
    }

    @Override
    public void destroy() {
        proxy.destroy();
    }

    @Override
    public String toString() {
        return "proxy:" + proxy.toString();
    }

}
