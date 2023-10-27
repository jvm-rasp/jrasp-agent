package com.jrasp.agent.core.client.socket;

import java.io.*;
import java.net.Socket;

import com.jrasp.agent.core.client.packet.*;
import com.jrasp.agent.core.newlog.LogUtil;

/**
 * 日志、指令传输
 *
 * @author jrasp
 * @since 2023/5/27
 */
public class RaspSocketImpl implements RaspSocket {

    private Socket sock;
    private String host;
    private int port;
    private volatile DataOutputStream output;
    private volatile DataInputStream input;
    private volatile boolean closed = true;

    public RaspSocketImpl(String host, int port) throws Exception {
        if (host == null) {
            throw new NullPointerException("host must not be null");
        }
        this.host = host;
        if (port == 0) {
            throw new IllegalArgumentException("Bad port: " + port);
        }
        this.port = port;
        connect();
    }

    @Override
    public void connect() throws Exception {
        sock = new Socket(host, port);
        setOutputStream(new DataOutputStream(sock.getOutputStream()));
        setInputStream(new DataInputStream(sock.getInputStream()));
        if (sock == null || sock.isClosed()) {
            closed = true;
            throw new RuntimeException("socket client is closed.");
        }
        closed = false;
    }

    @Override
    public void write(String msg, PacketType t) throws IOException {
        if (isClosed()) {
            return;
        }
        if (msg == null || "".equals(msg)) {
            return;
        }
        Packet p = new Packet(t, msg);
        try {
            Codec.INSTANCE.encode(output, p);
        } catch (Exception e) {
            LogUtil.warning("encode msg error", e);
            closed = true;
        }
    }

    @Override
    public Packet read() throws Exception {
        try {
            return Codec.INSTANCE.decode(input);
        } catch (EOFException e) {
            // 断开链接之后，抛出异常
            LogUtil.warning("server socket EOF when read", e);
            closed = true;
        } catch (Exception e) {
            // 解码异常
            LogUtil.warning("decode msg error", e);
        }
        return null;
    }

    @Override
    public synchronized void close() throws IOException {
        if (sock != null) {
            sock.close();
            sock = null;
            closed = true;
        }
    }

    @Override
    public synchronized boolean isClosed() {
        // TODO 判断是否正确
        return sock == null || closed || sock.isClosed();
    }

    private synchronized void setOutputStream(DataOutputStream out) throws Exception {
        if (out == null) {
            throw new NullPointerException("out must not be null");
        }
        flushAndCloseWrite();
        output = out;
    }

    private synchronized void setInputStream(DataInputStream in) throws Exception {
        if (in == null) {
            throw new NullPointerException("in must not be null");
        }
        flushAndCloseRead();
        input = in;
    }

    private synchronized void flushAndCloseWrite() throws Exception {
        if (output != null) {
            output.flush();
            output.close();
            output = null;
        }
    }

    private synchronized void flushAndCloseRead() throws Exception {
        if (input != null) {
            input.close();
            input = null;
        }
    }

}
