package com.jrasp.agent.core.classloader;

import util.encrypt.EncryptUtil;
import org.junit.Test;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class RoutingURLClassLoaderTest {



    private final String plainText = "This is test string";

    private final String encryptText = "hijEWo9VTf/L81TpUJHIL7pohZ0fxEG4lGJY4Hn72a8=";

    /**
     * 偏移量(CBC中使用，增强加密算法强度)
     */
    private final String IV = EncryptUtil.getMD5("My_IV").substring(8, 24);

    /**
     * 密码
     */
    private final String KEY = EncryptUtil.getMD5("My_Password").substring(8, 24);

    @Test
    public void aesTest() throws Exception {
        byte[] encryptData = EncryptUtil.encryptCBC(plainText.getBytes("UTF-8"), IV, KEY);
        String encrypted = new BASE64Encoder().encode(encryptData);
        assert encrypted.equals(encryptText);

        byte[] data = new BASE64Decoder().decodeBuffer(encryptText);
        byte[] decryptData = EncryptUtil.decryptCBC(data, IV, KEY);
        assert new String(decryptData).equals(plainText);
    }


}
