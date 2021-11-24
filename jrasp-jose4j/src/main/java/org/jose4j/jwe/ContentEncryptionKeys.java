package org.jose4j.jwe;

import org.jose4j.lang.ByteUtil;

/**
*/
public class ContentEncryptionKeys
{
    private final byte[] contentEncryptionKey;
    private final byte[] encryptedKey;

    public ContentEncryptionKeys(byte[] contentEncryptionKey, byte[] encryptedKey)
    {
        this.contentEncryptionKey = contentEncryptionKey;
        this.encryptedKey = encryptedKey == null ? ByteUtil.EMPTY_BYTES : encryptedKey;
    }

    public byte[] getContentEncryptionKey()
    {
        return contentEncryptionKey;
    }

    public byte[] getEncryptedKey()
    {
        return encryptedKey;
    }
}
