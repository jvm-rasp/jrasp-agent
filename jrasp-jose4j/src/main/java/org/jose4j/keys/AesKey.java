package org.jose4j.keys;

import org.jose4j.lang.ByteUtil;

import javax.crypto.spec.SecretKeySpec;

/**
 */
public class AesKey extends SecretKeySpec
{
    public static final String ALGORITHM = "AES";

    public AesKey(byte[] bytes)
    {
        super(bytes, ALGORITHM);
    }

    @Override
    public String toString()
    {
        return ByteUtil.bitLength(getEncoded().length) + " bit " + ALGORITHM + " key";
    }
}
