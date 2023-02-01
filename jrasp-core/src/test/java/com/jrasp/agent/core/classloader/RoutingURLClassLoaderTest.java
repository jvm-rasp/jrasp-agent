package com.jrasp.agent.core.classloader;

import util.encrypt.EncryptUtil;
import org.junit.Test;

public class RoutingURLClassLoaderTest {



    private final String plainText = "This is test string";
    private final byte[] encryptText = new byte[] {-122, 40, -60, 90, -113, 85, 77, -1, -53, -13, 84, -23, 80, -111, -56, 47, -70, 104, -123, -99, 31, -60, 65, -72, -108, 98, 88, -32, 121, -5, -39, -81};

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
        assert encryptData.length == encryptText.length;
        for(int i=0; i< encryptData.length; i++) {
            assert encryptData[i] == encryptText[i];
        }
        byte[] decryptData = EncryptUtil.decryptCBC(encryptText, IV, KEY);
        assert new String(decryptData).equals(plainText);
    }


}
