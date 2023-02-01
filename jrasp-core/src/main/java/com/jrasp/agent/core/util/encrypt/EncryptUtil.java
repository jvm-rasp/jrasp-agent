package com.jrasp.agent.core.util.encrypt;

import com.jrasp.agent.api.util.StringUtils;
import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;

public class EncryptUtil {
    private static Logger LOGGER = LoggerFactory.getLogger(EncryptUtil.class);
    /**
     * 编码
     */
    private static final String ENCODING = "UTF-8";

    /**
     * 算法定义
     */
    private static final String AES_ALGORITHM = "AES";

    /**
     * 指定填充方式
     */
    private static final String CIPHER_PADDING = "AES/ECB/PKCS5Padding";
    private static final String CIPHER_CBC_PADDING = "AES/CBC/PKCS5Padding";

    public static final String IV = EncryptUtil.getMD5("www.jrasp.com").substring(8, 24);

    public static String getMD5(String content) {
        StringBuilder buf = new StringBuilder("");
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(content.getBytes());//update处理
            byte [] encryContext = md.digest();//调用该方法完成计算
            int i;
            for (int offset = 0; offset < encryContext.length; offset++) {//做相应的转化（十六进制）
                i = encryContext[offset];
                if (i < 0) i += 256;
                if (i < 16) buf.append("0");
                buf.append(Integer.toHexString(i));
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return buf.toString();
    }

    /**
     * AES加密
     * @param content 待加密内容
     * @param aesKey  密码
     * @return
     */
    public static byte[] Encrypt(byte[] content, String aesKey){
        //判断秘钥是否为16位
        if(StringUtils.isNotBlank(aesKey) && aesKey.length() == 16){
            try {
                //对密码进行编码
                byte[] bytes = aesKey.getBytes(ENCODING);
                //设置加密算法，生成秘钥
                SecretKeySpec skeySpec = new SecretKeySpec(bytes, AES_ALGORITHM);
                // "算法/模式/补码方式"
                Cipher cipher = Cipher.getInstance(CIPHER_PADDING);
                //选择加密
                cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
                //根据待加密内容生成字节数组
                byte[] encrypted = cipher.doFinal(content);
                //返回base64字符串
                return encrypted;
            } catch (Exception e) {
                LOGGER.debug("AES encrypt exception:" + e.getMessage());
                throw new RuntimeException(e);
            }

        }else {
            LOGGER.error("AES encrypt: the aesKey is null or error!");
            return null;
        }
    }

    /**
     * 解密
     *
     * @param content 待解密内容
     * @param aesKey  密码
     * @return
     */
    public static byte[] Decrypt(byte[] content, String aesKey){
        //判断秘钥是否为16位
        if(StringUtils.isNotBlank(aesKey) && aesKey.length() == 16){
            try {
                //对密码进行编码
                byte[] bytes = aesKey.getBytes(ENCODING);
                //设置解密算法，生成秘钥
                SecretKeySpec skeySpec = new SecretKeySpec(bytes, AES_ALGORITHM);
                // "算法/模式/补码方式"
                Cipher cipher = Cipher.getInstance(CIPHER_PADDING);
                //选择解密
                cipher.init(Cipher.DECRYPT_MODE, skeySpec);

                //根据待解密内容进行解密
                byte[] decrypted = cipher.doFinal(content);
                //将字节数组转成字符串
                return decrypted;
            } catch (Exception e) {
                LOGGER.debug("AES decrypt exception:" + e.getMessage());
                throw new RuntimeException(e);
            }

        }else {
            LOGGER.error("AES decrypt: the aesKey is null or error!");
            return null;
        }
    }

    /**
     * AES_CBC加密
     *
     * @param content 待加密内容
     * @param aesKey  密码
     * @return
     */
    public static byte[] EncryptCBC(byte[] content, String ivSeed, String aesKey){
        byte[] encrypted = null;
        //判断秘钥是否为16位
        if(StringUtils.isNotBlank(aesKey) && aesKey.length() == 16){
            try {
                //对密码进行编码
                byte[] bytes = aesKey.getBytes(ENCODING);
                //偏移
                IvParameterSpec iv = new IvParameterSpec(ivSeed.getBytes(ENCODING));
                //设置加密算法，生成秘钥
                SecretKeySpec skeySpec = new SecretKeySpec(bytes, AES_ALGORITHM);
                // "算法/模式/补码方式"
                Cipher cipher = Cipher.getInstance(CIPHER_CBC_PADDING);
                //选择加密
                cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
                //根据待加密内容生成字节数组
                encrypted = cipher.doFinal(content);
            } catch (Exception e) {
                LOGGER.error("AES_CBC encrypt exception:" + e.getMessage());
                throw new RuntimeException(e);
            }
        }else {
            LOGGER.error("AES_CBC encrypt: the aesKey is null or error!");
        }
        return encrypted;
    }

    /**
     * AES_CBC解密
     *
     * @param content 待解密内容
     * @param aesKey  密码
     * @return
     */
    public static byte[] DecryptCBC(byte[] content, String ivSeed, String aesKey){
        byte[] decrypted = null;
        //判断秘钥是否为16位
        if(StringUtils.isNotBlank(aesKey) && aesKey.length() == 16){
            try {
                //对密码进行编码
                byte[] bytes = aesKey.getBytes(ENCODING);
                //偏移
                IvParameterSpec iv = new IvParameterSpec(ivSeed.getBytes(ENCODING));
                //设置解密算法，生成秘钥
                SecretKeySpec skeySpec = new SecretKeySpec(bytes, AES_ALGORITHM);
                // "算法/模式/补码方式"
                Cipher cipher = Cipher.getInstance(CIPHER_CBC_PADDING);
                //选择解密
                cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
                //根据待解密内容进行解密
                decrypted = cipher.doFinal(content);

            } catch (Exception e) {
                LOGGER.error("AES_CBC decrypt exception:" + e.getMessage());
                throw new RuntimeException(e);
            }
        } else {
            LOGGER.error("AES_CBC decrypt: the aesKey is null or error!");
        }
        return decrypted;
    }
}
