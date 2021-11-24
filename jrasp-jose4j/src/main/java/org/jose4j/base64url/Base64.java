package org.jose4j.base64url;

/**
 *
 */
public class Base64
{
    public static String encode(final byte[] bytes)
    {
        org.jose4j.base64url.internal.apache.commons.codec.binary.Base64 base64 = new org.jose4j.base64url.internal.apache.commons.codec.binary.Base64();
        return base64.encodeToString(bytes);
    }

    public static byte[] decode(final String encoded)
    {
        return org.jose4j.base64url.internal.apache.commons.codec.binary.Base64.decodeBase64(encoded);
    }
}
