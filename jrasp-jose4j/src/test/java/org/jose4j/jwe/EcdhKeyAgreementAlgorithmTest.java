package org.jose4j.jwe;

import junit.framework.TestCase;
import org.jose4j.base64url.Base64Url;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jwx.HeaderParameterNames;
import org.jose4j.jwx.Headers;
import org.jose4j.keys.AesKey;
import org.jose4j.lang.ByteUtil;
import org.jose4j.lang.JoseException;

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 */
public class EcdhKeyAgreementAlgorithmTest extends TestCase
{
    public void testExampleJwaAppendixC() throws JoseException
    {
        // testing http://tools.ietf.org/html/draft-ietf-jose-json-web-algorithms-17#appendix-D
        // now http://tools.ietf.org/html/draft-ietf-jose-json-web-algorithms-26#appendix-C
        String receiverJwkJson = "\n{\"kty\":\"EC\",\n" +
                " \"crv\":\"P-256\",\n" +
                " \"x\":\"weNJy2HscCSM6AEDTDg04biOvhFhyyWvOHQfeF_PxMQ\",\n" +
                " \"y\":\"e8lnCO-AlStT-NJVX-crhB7QRYhiix03illJOVAOyck\",\n" +
                " \"d\":\"VEmDZpDXXK8p8N0Cndsxs924q6nS1RXFASRl6BfUqdw\"\n" +
                "}";
        PublicJsonWebKey receiverJwk = PublicJsonWebKey.Factory.newPublicJwk(receiverJwkJson);

        String ephemeralJwkJson = "\n{\"kty\":\"EC\",\n" +
                " \"crv\":\"P-256\",\n" +
                " \"x\":\"gI0GAILBdu7T53akrFmMyGcsF3n5dO7MmwNBHKW5SV0\",\n" +
                " \"y\":\"SLW_xSffzlPWrHEVI30DHM_4egVwt3NQqeUD7nMFpps\",\n" +
                " \"d\":\"0_NxaRPUMQoAJt50Gz8YiTr8gRTwyEaCumd-MToTmIo\"\n" +
                "}";

        PublicJsonWebKey ephemeralJwk = PublicJsonWebKey.Factory.newPublicJwk(ephemeralJwkJson);

        Headers headers = new Headers();

        headers.setStringHeaderValue(HeaderParameterNames.ALGORITHM, KeyManagementAlgorithmIdentifiers.ECDH_ES);
        headers.setStringHeaderValue(HeaderParameterNames.ENCRYPTION_METHOD, ContentEncryptionAlgorithmIdentifiers.AES_128_GCM);

        headers.setStringHeaderValue(HeaderParameterNames.AGREEMENT_PARTY_U_INFO, "QWxpY2U");
        headers.setStringHeaderValue(HeaderParameterNames.AGREEMENT_PARTY_V_INFO, "Qm9i");

        headers.setJwkHeaderValue(HeaderParameterNames.EPHEMERAL_PUBLIC_KEY, ephemeralJwk);

        EcdhKeyAgreementAlgorithm ecdhKeyAgreementAlgorithm = new EcdhKeyAgreementAlgorithm();

        ContentEncryptionKeyDescriptor cekDesc = new ContentEncryptionKeyDescriptor(ByteUtil.byteLength(128), AesKey.ALGORITHM);

        PublicKey pubKey = receiverJwk.getPublicKey();
        ContentEncryptionKeys contentEncryptionKeys = ecdhKeyAgreementAlgorithm.manageForEncrypt(pubKey, cekDesc, headers, ephemeralJwk);

        assertTrue(contentEncryptionKeys.getEncryptedKey().length == 0);
        Base64Url base64Url = new Base64Url();
        assertEquals("VqqN6vgjbSBcIijNcacQGg", base64Url.base64UrlEncode(contentEncryptionKeys.getContentEncryptionKey()));

        Headers receivedHeaders = new Headers();
        receivedHeaders.setFullHeaderAsJsonString(headers.getFullHeaderAsJsonString());

        Key key = ecdhKeyAgreementAlgorithm.manageForDecrypt(receiverJwk.getPrivateKey(), null, cekDesc, receivedHeaders);
        assertEquals("VqqN6vgjbSBcIijNcacQGg", base64Url.base64UrlEncode(key.getEncoded()));
    }

        public void testDV256() throws JoseException
        {
        /*
            A working test w/ data produced by Dmitry Vsekhvalnov doing ECDH with P-256 + ConcatKDF to produce a 256 bit key
            ---
            Ok, data below. Everything base64url encoded. partyUInfo=partyVInfo=[0,0,0,0] in all samples.

            Curve P-256, 256 bit key (match to jose4j and to spec sample, provided as reference)

            X = BHId3zoDv6pDgOUh8rKdloUZ0YumRTcaVDCppUPoYgk
            Y = g3QIDhaWEksYtZ9OWjNHn9a6-i_P9o5_NrdISP0VWDU
            D = KpTnMOHEpskXvuXHFCfiRtGUHUZ9Dq5CCcZQ-19rYs4

            ephemeral X = UWlKW_GHsZa1ikOUPocsMi2pNh_1K2vhn6ZjJqALOK8
            ephemeral Y = n2oj0Z6EYgzRDmeROILD4fp2zAMGLQzmI8G1k5nsev0

            algId = AAAADUExMjhDQkMtSFMyNTY
            suppPubInfo = AAABAA

            derived key = bqXVMd1yd5E08Wy2T1U9m9Q5DEjj7-BYIyWUgazzZkA
         */

        String receiverJwkJson = "\n{\"kty\":\"EC\",\n" +
                " \"crv\":\"P-256\",\n" +
                " \"x\":\"BHId3zoDv6pDgOUh8rKdloUZ0YumRTcaVDCppUPoYgk\",\n" +
                " \"y\":\"g3QIDhaWEksYtZ9OWjNHn9a6-i_P9o5_NrdISP0VWDU\",\n" +
                " \"d\":\"KpTnMOHEpskXvuXHFCfiRtGUHUZ9Dq5CCcZQ-19rYs4\"\n" +
                "}";
        PublicJsonWebKey receiverJwk = PublicJsonWebKey.Factory.newPublicJwk(receiverJwkJson);

        String ephemeralJwkJson = "\n{\"kty\":\"EC\",\n" +
                " \"crv\":\"P-256\",\n" +
                " \"x\":\"UWlKW_GHsZa1ikOUPocsMi2pNh_1K2vhn6ZjJqALOK8\",\n" +
                " \"y\":\"n2oj0Z6EYgzRDmeROILD4fp2zAMGLQzmI8G1k5nsev0\"\n" +
                "}";

        PublicJsonWebKey ephemeralJwk = PublicJsonWebKey.Factory.newPublicJwk(ephemeralJwkJson);

        Headers headers = new Headers();

        headers.setStringHeaderValue(HeaderParameterNames.ALGORITHM, KeyManagementAlgorithmIdentifiers.ECDH_ES);
        headers.setStringHeaderValue(HeaderParameterNames.ENCRYPTION_METHOD, ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);

        headers.setJwkHeaderValue(HeaderParameterNames.EPHEMERAL_PUBLIC_KEY, ephemeralJwk);

        EcdhKeyAgreementAlgorithm ecdhKeyAgreementAlgorithm = new EcdhKeyAgreementAlgorithm();

        ContentEncryptionKeyDescriptor cekDesc = new ContentEncryptionKeyDescriptor(ByteUtil.byteLength(256), AesKey.ALGORITHM);

        Key derivedKey = ecdhKeyAgreementAlgorithm.manageForDecrypt(receiverJwk.getPrivateKey(), null, cekDesc, headers);
        assertEquals("bqXVMd1yd5E08Wy2T1U9m9Q5DEjj7-BYIyWUgazzZkA", Base64Url.encode(derivedKey.getEncoded()));
    }

    public void testDecryptPrecomputedP256_ECDHandAES_256_CBC_HMAC_SHA_512() throws Exception
    {
        PublicJsonWebKey jwk = PublicJsonWebKey.Factory.newPublicJwk("{\"kty\":\"EC\",\"x\":\"fXx-DfOsmecjKh3VrLZFsF98Z1nutsL4UdFTdgA8S7Y\"," +
                "\"y\":\"LGzyJY99aqKk52UIExcNFSTs0S7HnNzQ-DRWBTHDad4\",\"crv\":\"P-256\",\"d\":\"OeVCWbXuFuJ9U16q7bhLNoKPLLnK-yTx95grzfvQ2l4\"}");
        JsonWebEncryption jwe = new JsonWebEncryption();
        String cs =
                "eyJlbmMiOiJBMjU2Q0JDLUhTNTEyIiwiYWxnIjoiRUNESC1FUyIsImVwayI6eyJrdHkiOiJFQyIsIngiOiJ3ZlRHNVFHZkItNHUxanVUUEN1aTNESXhFTV" +
                        "82ZUs5ZEk5TXNZckpxWDRnIiwieSI6Ik8yanlRbHQ2TXFGTGtqMWFCWW1aNXZJWHFVRHh6Ulk3dER0WmdZUUVNa0kiLCJjcnYiOiJQLTI1NiJ9fQ." +
                        "." +
                        "mk4wQzGSSeZ8uSgEYTIetA." +
                        "fCw3-TosL4p0D5fEXw0bEA." +
                        "9mPsdmGTVoVexXqEOdN5VUKk-ZNtfOtUfbdjVHoko_o";
        jwe.setCompactSerialization(cs);
        jwe.setKey(jwk.getPrivateKey());
        assertEquals("It works!", jwe.getPayload());
    }

    public void testDecryptPrecomputedP384_ECDHandAES_192_CBC_HMAC_SHA_384() throws Exception
    {
        PublicJsonWebKey jwk = PublicJsonWebKey.Factory.newPublicJwk("{\"kty\":\"EC\",\"x\":\"nBr92fh2JsEjIF1LR5PKICBeHNIBe0xb7nlBrrU3WoWgfJYfXve1jxC-5VT5EPLt\"," +
                "\"y\":\"sUAxL3L5lJdzFUSR9EHLniuBhEbvXfPa_3OiR6Du0_GOlFXXIi4UmbNpk10_Thfq\"," +
                "\"crv\":\"P-384\",\"d\":\"0f0NnWg__Qgqjj3fl2gAlsID4Ni41FR88cmZPVgb6ch-ZShuVJRjoxymCuzVP7Gi\"}");
        JsonWebEncryption jwe = new JsonWebEncryption();
        String cs = "eyJlbmMiOiJBMTkyQ0JDLUhTMzg0IiwiYWxnIjoiRUNESC1FUyIsImVwayI6eyJrdHkiOiJFQyIsIngiOiJsX3hXdzIyb1N" +
                "fOWZGbV96amNzYkstd3R3d0RHSlRQLUxnNFVBWDI3WWF1b1YwNml2emwtcm1ra2h6ci11SDBmIiwieSI6IloyYmVn" +
                "bzBqeE9nY0YtNVp4SFNBOU5jZDVCOW8wUE1pSVlRbm9sWkNQTHA3YndPd1RLUEZaaFZVUlFPSjdoeUciLCJjcnYiOiJQLTM4NCJ9fQ." +
                ".jSWP7pfa4KcpqKWZ1x8awg.osb-5641Ej1Uon_f3U8bNw.KUQWwb35Gxq3YQ34_AVkebugx4rxq1lO\n";
        jwe.setCompactSerialization(cs);
        jwe.setKey(jwk.getPrivateKey());
        assertEquals("Please work...", jwe.getPayload());
    }

    public void testDecryptPrecomputedP521_ECDHandAES_256_CBC_HMAC_SHA_512() throws Exception
    {
        PublicJsonWebKey jwk = PublicJsonWebKey.Factory.newPublicJwk("{\"kty\":\"EC\"," +
                "\"x\":\"AH3rqSYjKue50ThW0qq_qQ76cNtqWrc7hU6kZR6akxy8iTf8ugcpqnbgbi98AgSwIqgJZDBMCk-8eoiGaf3R_kDD\"," +
                "\"y\":\"AeafPdJjHLf6pK5V7iyMsL3-6MShpHS6jXQ8m-Bcbp06yxAMn6TJbdkacvj45dy_pdh1s6XZwoxRxNETg_gj-hq9\"," +
                "\"crv\":\"P-521\"," +
                "\"d\":\"AB2tm9vgGe2BaxZmJQ016GY-U7NV_EWhrPsLDC5l9tAM9DGEwI2cT2HcO20Z6CQndw0ZhqLZ6MEvS8siL-SCxIl2\"}\n");
        JsonWebEncryption jwe = new JsonWebEncryption();
        String cs = "eyJlbmMiOiJBMjU2Q0JDLUhTNTEyIiwiYWxnIjoiRUNESC1FUyIsImVwayI6eyJrdHkiOiJFQyIsIngiOiJBQ1RLMlVPSjJ6SVk3U1U4T0xkaG1QQmE4ZUVpd2JrX0" +
                "9UMXE0MHBsRlRwQmJKUXg3YWdqWG9LYml2NS1OTXB6eXZySm1rblM3SjNRUWlUeFgwWmtjemhEIiwieSI6IkFXeTZCR1dkZld2ekVNeGIxQklCQnZmRDJ4bEh6Rjk2YzVVR" +
                "VQ4SFBUS0RSeUJyMnQ4T2dTX1J2MnNoUmxGbXlqUWpyX25uQk94akcxVTZNWDNlZ2VETzciLCJjcnYiOiJQLTUyMSJ9fQ..EWqSGntxbO_Y_6JRjFkCgg.DGjDNjAYdsnYT" +
                "pUFJi1gEI4YtNd7gBPMjD3CDH047RAwZKTme6Ah_ztzxSfVg5kG.yGm5jn2LtbFXaK_yf0b0932sI2O77j2gwmL1Y09YC_Y";
        jwe.setCompactSerialization(cs);
        jwe.setKey(jwk.getPrivateKey());
        assertEquals("And also the working here would be nice.", jwe.getPayload());
    }
}
