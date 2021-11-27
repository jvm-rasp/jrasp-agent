package com.jrasp.provider;

import com.jrasp.api.ConfigInfo;
import com.jrasp.provider.api.ModuleJarLoadingChain;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jrasp.api.Resource;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;

public class EmptyModuleJarLoadingChain implements ModuleJarLoadingChain {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private ConfigInfo configInfo;

    @Override
    public void loading(File moduleJarFile) throws Throwable {
        String key = configInfo.getEncryptionkey(); // 产生 key
        if (key != null && key.length() > 0 && key.trim().length() > 0) { // 密钥不为空时，解密
            // 解密 jar 包，解密失败后抛出异常，解密失败的jar终止加载流程
            byte[] bytes1 = FileUtils.readFileToByteArray(moduleJarFile);
            byte[] decrypt = decrypt(bytes1, key);
            FileUtils.writeByteArrayToFile(moduleJarFile, decrypt); // 直接覆盖，不是追加
        }
    }

    /**
     * 加密用的Key 可以用26个字母和数字组成
     * 此处使用AES-128-CBC加密模式，key需要为16位。
     */
    private static String ivParameter = "1234567890123456";  // 偏移量

    // 解密
    public static byte[] decrypt(byte[] body, String key) throws Exception {
        byte[] raw = key.getBytes();
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec iv = new IvParameterSpec(ivParameter.getBytes());
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
        return cipher.doFinal(body);
    }
}
