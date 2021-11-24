package org.jose4j.jwe;

import junit.framework.TestCase;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwx.HeaderParameterNames;
import org.jose4j.keys.AesKey;
import org.jose4j.lang.ByteUtil;
import org.jose4j.lang.InvalidAlgorithmException;
import org.jose4j.lang.JoseException;
import org.jose4j.zip.CompressionAlgorithmIdentifiers;

/**
 */
public class ZipTest extends TestCase
{
    public void testJweRoundTripWithAndWithoutZip() throws JoseException
    {
        JsonWebEncryption jwe = new JsonWebEncryption();
        String plaintext = "This should compress pretty good, it should, yes it should pretty good it should" +
                " pretty good it should it should it should it should pretty good it should pretty good it should" +
                " pretty good it should pretty good it should pretty good it should pretty good it should pretty good.";
        jwe.setPlaintext(plaintext);
        AesKey key = new AesKey(ByteUtil.randomBytes(32));
        jwe.setKey(key);
        jwe.setHeader(HeaderParameterNames.ZIP, CompressionAlgorithmIdentifiers.DEFLATE);
        jwe.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.DIRECT);
        jwe.setEncryptionMethodHeaderParameter(ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);

        String csWithZip = jwe.getCompactSerialization();
        System.out.println(csWithZip);

        jwe = new JsonWebEncryption();
        jwe.setPlaintext(plaintext);
        jwe.setKey(key);
        jwe.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.DIRECT);
        jwe.setEncryptionMethodHeaderParameter(ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);

        String csWithOutZip = jwe.getCompactSerialization();
        System.out.println(csWithOutZip);

        assertTrue(csWithZip.length() < csWithOutZip.length());

        JsonWebEncryption decryptingJwe = new JsonWebEncryption();
        decryptingJwe.setKey(key);
        decryptingJwe.setCompactSerialization(csWithZip);
        String plaintextString = decryptingJwe.getPlaintextString();
        assertEquals(plaintext, plaintextString);

        decryptingJwe = new JsonWebEncryption();
        decryptingJwe.setKey(key);
        decryptingJwe.setCompactSerialization(csWithOutZip);
        plaintextString = decryptingJwe.getPlaintextString();
        assertEquals(plaintext, plaintextString);
    }

    public void testJweBadZipValueProduce() throws JoseException
    {
        JsonWebEncryption jwe = new JsonWebEncryption();
        String plaintext = "This should compress pretty good, it should, yes it should pretty good it should it should";
        jwe.setPlaintext(plaintext);
        AesKey key = new AesKey(new byte[32]);
        jwe.setKey(key);
        jwe.setHeader(HeaderParameterNames.ZIP, "bad");
        jwe.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.DIRECT);
        jwe.setEncryptionMethodHeaderParameter(ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);

        try
        {
            String cs = jwe.getCompactSerialization();
            fail("Should fail with invalid zip header value: " + cs);
        }
        catch (InvalidAlgorithmException e)
        {
            // just see if the exception message says something about the header name
            assertTrue(e.getMessage().contains(HeaderParameterNames.ZIP));
        }
    }

    public void testJwBadZipValueConsume() throws JoseException
    {
        String cs = "eyJ6aXAiOiJiYWQiLCJhbGciOiJkaXIiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2In0.." +
                "ZZZ0nR5f80ikJtaPot4RpQ." +
                "BlDAYKzn9oLH1fhZcR60ZKye7UHslg7s0h7s1ecNZ5A1Df1pq2pBWUwdRKjJRxJAEFbDFoXTFYjV-cLCCE2Uxw." +
                "zasDvsZ3U4YkTDgIUchjiA";

        JsonWebKey jsonWebKey = JsonWebKey.Factory.newJwk("{\"kty\":\"oct\",\"k\":\"q1qm8z2sLFt_CPqwpLuGm-fX6ZKQKnukPHpoJOeykCw\"}");

        JsonWebEncryption jwe = new JsonWebEncryption();
        jwe.setKey(jsonWebKey.getKey());
        jwe.setCompactSerialization(cs);

        try
        {
            String plaintextString = jwe.getPlaintextString();
            fail("Should fail with invalid zip header value but gave: " + plaintextString);
        }
        catch (InvalidAlgorithmException e)
        {
            // just see if the exception message says something about the header name
            assertTrue(e.getMessage().contains(HeaderParameterNames.ZIP));
        }

    }
}
