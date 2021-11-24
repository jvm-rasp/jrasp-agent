package org.jose4j.jwe;

import junit.framework.TestCase;
import org.jose4j.base64url.Base64Url;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.keys.AesKey;
import org.jose4j.lang.ByteUtil;
import org.jose4j.lang.JoseException;

import java.security.Key;
import java.util.Arrays;

/**
 */
public class Aes128KeyWrapManagementAlgorithmTest extends TestCase
{
    public void testJweExample() throws JoseException
    {
        // Test the AES key wrap part of Example JWE using AES Key Wrap and AES_128_CBC_HMAC_SHA_256 from
        // http://tools.ietf.org/html/draft-ietf-jose-json-web-encryption-14#appendix-A.3

        int[] cekInts = {4, 211, 31, 197, 84, 157, 252, 254, 11, 100, 157, 250, 63, 170, 106,
                206, 107, 124, 212, 45, 111, 107, 9, 219, 200, 177, 0, 240, 143, 156,
                44, 207};
        byte[] cekBytes = ByteUtil.convertUnsignedToSignedTwosComp(cekInts);

        JsonWebKey jsonWebKey = JsonWebKey.Factory.newJwk("\n" +
                "     {\"kty\":\"oct\",\n" +
                "      \"k\":\"GawgguFyGrWKav7AX4VKUg\"\n" +
                "     }");
        AesKey managementKey = new AesKey(jsonWebKey.getKey().getEncoded());

        WrappingKeyManagementAlgorithm wrappingKeyManagementAlgorithm = new AesKeyWrapManagementAlgorithm.Aes128();

        ContentEncryptionAlgorithm contentEncryptionAlgorithm = new AesCbcHmacSha2ContentEncryptionAlgorithm.Aes128CbcHmacSha256();
        ContentEncryptionKeyDescriptor cekDesc = contentEncryptionAlgorithm.getContentEncryptionKeyDescriptor();

        ContentEncryptionKeys contentEncryptionKeys = wrappingKeyManagementAlgorithm.manageForEnc(managementKey, cekDesc, cekBytes);

        String encodedEncryptedKeyFromExample ="6KB707dM9YTIgHtLvtgWQ8mKwboJW3of9locizkDTHzBC2IlrT1oOQ";

        Base64Url u = new Base64Url();
        String encodedWrapped = u.base64UrlEncode(contentEncryptionKeys.getEncryptedKey());

        assertEquals(encodedEncryptedKeyFromExample, encodedWrapped);

        byte[] encryptedKey = u.base64UrlDecode(encodedEncryptedKeyFromExample);

        Key key = wrappingKeyManagementAlgorithm.manageForDecrypt(managementKey, encryptedKey, cekDesc, null);

        assertTrue(Arrays.equals(cekBytes, key.getEncoded()));
    }
}
