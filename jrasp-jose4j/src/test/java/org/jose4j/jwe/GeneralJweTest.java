package org.jose4j.jwe;

import org.jose4j.keys.ExampleRsaJwksFromJwe;
import org.jose4j.lang.JoseException;
import org.junit.Test;

/**
 *
 */
public class GeneralJweTest
{
    @Test(expected = NullPointerException.class)
    public void tryEncryptWithNullPlainText() throws JoseException
    {
        // I think it's probably correct to fail when encrypting and the plaintext is null
        // but should fail so in a way that's not confusing
        // it was, at one point, erroneously trying to decrypt inside of jwe.getCompactSerialization()
        // when it saw that the plantext bytes were null and then threw a misleading exception about
        // key validation
        JsonWebEncryption jwe = new JsonWebEncryption();
        jwe.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.RSA1_5);
        jwe.setKeyIdHeaderValue("meh");
        jwe.setEncryptionMethodHeaderParameter(ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);
        jwe.setKey(ExampleRsaJwksFromJwe.APPENDIX_A_1.getPublicKey());
        String compactSerialization = jwe.getCompactSerialization();
        System.out.println(compactSerialization);
    }
}
