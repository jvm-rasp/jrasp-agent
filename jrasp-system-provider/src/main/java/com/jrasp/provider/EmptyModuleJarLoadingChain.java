package com.jrasp.provider;

import com.jrasp.api.ConfigInfo;
import com.jrasp.provider.api.ModuleJarLoadingChain;

import com.jrasp.api.Resource;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;

public class EmptyModuleJarLoadingChain implements ModuleJarLoadingChain {

    @Resource
    private ConfigInfo configInfo;

    @Override
    public void loading(File moduleJarFile) throws Throwable {
        String key = configInfo.getEncryptionkey(); // 产生 key
        if (key != null && key.length() > 0 && key.trim().length() > 0) { // 密钥不为空时，解密
            // 解密 jar 包，解密失败后抛出异常，解密失败的jar终止加载流程
            byte[] bytes1 = readFileToByteArray(moduleJarFile);
            byte[] decrypt = decrypt(bytes1, key);
            writeByteArrayToFile(moduleJarFile, decrypt, false); // 直接覆盖，不是追加
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

    // ---------------------------------------以下代码来源于 org.apache.commons.io----------------------------------------

    public static void writeByteArrayToFile(File file, byte[] data, boolean append) throws IOException {
        OutputStream out = null;
        try {
            out = openOutputStream(file, append);
            out.write(data);
            out.close(); // don't swallow close Exception if copy completes normally
        } finally {
            closeQuietly(out);
        }
    }

    public static FileOutputStream openOutputStream(File file, boolean append) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File '" + file + "' exists but is a directory");
            }
            if (file.canWrite() == false) {
                throw new IOException("File '" + file + "' cannot be written to");
            }
        } else {
            File parent = file.getParentFile();
            if (parent != null) {
                if (!parent.mkdirs() && !parent.isDirectory()) {
                    throw new IOException("Directory '" + parent + "' could not be created");
                }
            }
        }
        return new FileOutputStream(file, append);
    }

    public static byte[] readFileToByteArray(File file) throws IOException {
        InputStream in = null;
        try {
            in = openInputStream(file);
            return toByteArray(in, file.length());
        } finally {
            closeQuietly(in);
        }
    }

    public static FileInputStream openInputStream(File file) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File '" + file + "' exists but is a directory");
            }
            if (file.canRead() == false) {
                throw new IOException("File '" + file + "' cannot be read");
            }
        } else {
            throw new FileNotFoundException("File '" + file + "' does not exist");
        }
        return new FileInputStream(file);
    }

    public static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ioe) {
            // ignore
        }
    }

    public static byte[] toByteArray(InputStream input, long size) throws IOException {
        if (size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Size cannot be greater than Integer max value: " + size);
        }
        return toByteArray(input, (int) size);
    }

    public static byte[] toByteArray(InputStream input, int size) throws IOException {

        if (size < 0) {
            throw new IllegalArgumentException("Size must be equal or greater than zero: " + size);
        }

        if (size == 0) {
            return new byte[0];
        }

        byte[] data = new byte[size];
        int offset = 0;
        int readed;

        while (offset < size && (readed = input.read(data, offset, size - offset)) != -1) {
            offset += readed;
        }

        if (offset != size) {
            throw new IOException("Unexpected readed size. current: " + offset + ", excepted: " + size);
        }

        return data;
    }
}
