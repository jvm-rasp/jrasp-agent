package org.jose4j.jwe;

import junit.framework.TestCase;
import org.jose4j.base64url.Base64Url;
import org.jose4j.keys.AesKey;
import org.jose4j.keys.ExampleRsaJwksFromJwe;
import org.jose4j.lang.ByteUtil;
import org.jose4j.lang.JoseException;

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

/**
 */
public class Rsa1_5KeyManagementAlgorithmTest extends TestCase
{
    public void testJweExampleA2() throws JoseException
    {
        String encodedEncryptedKey =
                "UGhIOguC7IuEvf_NPVaXsGMoLOmwvc1GyqlIKOK1nN94nHPoltGRhWhw7Zx0-kFm" +
                "1NJn8LE9XShH59_i8J0PH5ZZyNfGy2xGdULU7sHNF6Gp2vPLgNZ__deLKxGHZ7Pc" +
                "HALUzoOegEI-8E66jX2E4zyJKx-YxzZIItRzC5hlRirb6Y5Cl_p-ko3YvkkysZIF" +
                "NPccxRU7qve1WYPxqbb2Yw8kZqa2rMWI5ng8OtvzlV7elprCbuPhcCdZ6XDP0_F8" +
                "rkXds2vE4X-ncOIM8hAYHHi29NX0mcKiRaD0-D-ljQTP-cFPgwCp6X-nZZd9OHBv" +
                "-B3oWh2TbqmScqXMR4gp_A";
        Base64Url base64Url = new Base64Url();
        byte[] encryptedKey = base64Url.base64UrlDecode(encodedEncryptedKey);

        RsaKeyManagementAlgorithm.Rsa1_5 keyManagementAlgorithm = new RsaKeyManagementAlgorithm.Rsa1_5();
        PrivateKey privateKey = ExampleRsaJwksFromJwe.APPENDIX_A_2.getPrivateKey();
        ContentEncryptionAlgorithm contentEncryptionAlgorithm = new AesCbcHmacSha2ContentEncryptionAlgorithm.Aes128CbcHmacSha256();
        ContentEncryptionKeyDescriptor cekDesc = contentEncryptionAlgorithm.getContentEncryptionKeyDescriptor();
        Key key = keyManagementAlgorithm.manageForDecrypt(privateKey, encryptedKey, cekDesc, null);

        byte[] cekBytes = ByteUtil.convertUnsignedToSignedTwosComp(new int[]{4, 211, 31, 197, 84, 157, 252, 254, 11, 100, 157, 250, 63, 170, 106,
                206, 107, 124, 212, 45, 111, 107, 9, 219, 200, 177, 0, 240, 143, 156,
                44, 207});

        byte[] encoded = key.getEncoded();
        assertTrue(Arrays.toString(encoded), Arrays.equals(cekBytes, encoded));
    }

    public void testRoundTrip() throws JoseException
    {
        RsaKeyManagementAlgorithm.Rsa1_5 rsa = new RsaKeyManagementAlgorithm.Rsa1_5();
        ContentEncryptionKeyDescriptor cekDesc = new ContentEncryptionKeyDescriptor(16, AesKey.ALGORITHM);
        PublicKey publicKey = ExampleRsaJwksFromJwe.APPENDIX_A_1.getPublicKey();
        ContentEncryptionKeys contentEncryptionKeys = rsa.manageForEncrypt(publicKey, cekDesc, null, null);

        byte[] encryptedKey = contentEncryptionKeys.getEncryptedKey();

        PrivateKey privateKey = ExampleRsaJwksFromJwe.APPENDIX_A_1.getPrivateKey();
        Key key = rsa.manageForDecrypt(privateKey, encryptedKey, cekDesc, null);

        byte[] cek = contentEncryptionKeys.getContentEncryptionKey();
        assertTrue(Arrays.equals(cek, key.getEncoded()));
    }
}
