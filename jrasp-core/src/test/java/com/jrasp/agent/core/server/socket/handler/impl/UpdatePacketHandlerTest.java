package com.jrasp.agent.core.server.socket.handler.impl;

import com.jrasp.agent.core.util.FeatureCodec;
import org.junit.Test;

import java.util.Map;

public class UpdatePacketHandlerTest {

    private static final FeatureCodec kv = new FeatureCodec(';', '=');

    @Test
    public void test() {
        String parameters = "file-algorithm:file_list_action=0;file_read_action=0;file_upload_action=0;danger_dir_list=/,/home,/etc,/usr,/usr/local,/var/log,/proc,/sys,/root,C:\\\\,D:\\\\,E:\\\\;file_upload_black_list=.jsp,.asp,.phar,.phtml,.sh,.py,.pl,.rb;travel_str=../,..\\\\;file_delete_action=0;";
        Map<String, String> parametersMap = kv.toMap(parameters);
        assert parametersMap.size() == 7;
    }
}
