package org.jose4j.jwk;

import org.jose4j.keys.AesKey;
import org.jose4j.lang.ByteUtil;

/**
 */
public class OctJwkGenerator
{
    public static OctetSequenceJsonWebKey generateJwk(int keyLengthInBits)
    {
        byte[] bytes = ByteUtil.randomBytes(ByteUtil.byteLength(keyLengthInBits));
        return new OctetSequenceJsonWebKey(new AesKey(bytes));
    }
}
