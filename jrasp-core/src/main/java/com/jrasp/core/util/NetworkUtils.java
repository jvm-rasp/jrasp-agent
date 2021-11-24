package com.jrasp.core.util;

import org.apache.commons.io.IOUtils;

import java.net.InetAddress;
import java.net.Socket;

public class NetworkUtils {

    public static boolean isPortInUsing(String host, int port) {
        Socket socket = null;
        try {
            final InetAddress Address = InetAddress.getByName(host);
            socket = new Socket(Address, port);  //建立一个Socket连接
            return socket.isConnected();
        } catch (Throwable cause) {
            // ignore
        } finally {
            if (null != socket) {
                IOUtils.closeQuietly(socket);
            }
        }
        return false;
    }

}
