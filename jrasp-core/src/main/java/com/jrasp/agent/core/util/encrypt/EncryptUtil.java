package com.jrasp.agent.core.util.encrypt;

import com.jrasp.agent.core.util.string.RaspStringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EncryptUtil {

    private final static Logger logger = Logger.getLogger(EncryptUtil.class.getName());

    /**
     * 解密
     * TODO 解密算法的jdk兼容性
     *
     * @param content
     * @param aesKey
     * @return
     */
    public static byte[] decrypt(byte[] content, String aesKey) {
        if (RaspStringUtils.isBlank(aesKey) || aesKey.length() != 16) {
            logger.log(Level.WARNING, "AES decrypt: the aesKey is null or error!");
            return null;
        }
        try {
            byte[] bytes = aesKey.getBytes("UTF-8");
            SecretKeySpec skeySpec = new SecretKeySpec(bytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            return cipher.doFinal(content);
        } catch (Exception e) {
            logger.log(Level.WARNING, "AES decrypt exception:" + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
