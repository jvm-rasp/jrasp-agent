/*
 * Copyright 2012-2014 Brian Campbell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jose4j.jwe;

import org.jose4j.base64url.Base64Url;
import org.jose4j.jwx.HeaderParameterNames;
import org.jose4j.jwx.Headers;
import org.jose4j.keys.PbkdfKey;
import org.jose4j.lang.ByteUtil;
import org.jose4j.lang.InvalidKeyException;
import org.jose4j.lang.JoseException;
import org.junit.Test;

import java.security.Key;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers.*;
import static org.jose4j.jwe.KeyManagementAlgorithmIdentifiers.PBES2_HS256_A128KW;
import static org.jose4j.jwe.KeyManagementAlgorithmIdentifiers.PBES2_HS384_A192KW;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 */
public class Pbes2HmacShaWithAesKeyWrapAlgorithmTest
{
    // per http://tools.ietf.org/html/draft-ietf-jose-json-web-algorithms-23#section-4.8.1.2
    // "A minimum iteration count of 1000 is RECOMMENDED."
    public static final int MINIMUM_ITERAION_COUNT = 1000;

    // per tools.ietf.org/html/draft-ietf-jose-json-web-algorithms-23#section-4.8.1.1
    // "A Salt Input value containing 8 or more octets MUST be used"
    public static final int MINIMUM_SALT_BYTE_LENGTH = 8;

    @Test
    public void combinationOfRoundTrips() throws Exception
    {
        String[] algs = new String[] {PBES2_HS256_A128KW, PBES2_HS384_A192KW, PBES2_HS256_A128KW};
        String[] encs = new String[] {AES_128_CBC_HMAC_SHA_256, AES_192_CBC_HMAC_SHA_384, AES_256_CBC_HMAC_SHA_512};

        String password = "password";
        String plaintext = "<insert some witty quote or remark here>";

        for (String alg : algs)
        {
            for (String enc : encs)
            {
                JsonWebEncryption encryptingJwe  = new JsonWebEncryption();
                encryptingJwe.setAlgorithmHeaderValue(alg);
                encryptingJwe.setEncryptionMethodHeaderParameter(enc);
                encryptingJwe.setPayload(plaintext);
                encryptingJwe.setKey(new PbkdfKey(password));
                String compactSerialization = encryptingJwe.getCompactSerialization();

                JsonWebEncryption decryptingJwe = new JsonWebEncryption();
                decryptingJwe.setCompactSerialization(compactSerialization);
                decryptingJwe.setKey(new PbkdfKey(password));
                assertThat(plaintext, equalTo(decryptingJwe.getPayload()));
            }
        }
    }

    @Test (expected = InvalidKeyException.class)
    public void testNullKey() throws JoseException
    {
        JsonWebEncryption encryptingJwe  = new JsonWebEncryption();
        encryptingJwe.setAlgorithmHeaderValue(PBES2_HS256_A128KW);
        encryptingJwe.setEncryptionMethodHeaderParameter(AES_128_CBC_HMAC_SHA_256);
        encryptingJwe.setPayload("meh");

        encryptingJwe.getCompactSerialization();
    }

    @Test
    public void testDefaultsMeetMinimumRequiredOrSuggested() throws JoseException
    {
        JsonWebEncryption encryptingJwe  = new JsonWebEncryption();
        encryptingJwe.setAlgorithmHeaderValue(PBES2_HS256_A128KW);
        encryptingJwe.setEncryptionMethodHeaderParameter(AES_128_CBC_HMAC_SHA_256);
        encryptingJwe.setPayload("meh");
        PbkdfKey key = new PbkdfKey("passtheword");
        encryptingJwe.setKey(key);
        String compactSerialization = encryptingJwe.getCompactSerialization();
        System.out.println(compactSerialization);

        JsonWebEncryption decryptingJwe = new JsonWebEncryption();
        decryptingJwe.setCompactSerialization(compactSerialization);
        decryptingJwe.setKey(key);
        decryptingJwe.getPayload();
        Headers headers = decryptingJwe.getHeaders();

        Long iterationCount = headers.getLongHeaderValue(HeaderParameterNames.PBES2_ITERATION_COUNT);
        assertTrue(iterationCount >= MINIMUM_ITERAION_COUNT);

        String saltInputString = headers.getStringHeaderValue(HeaderParameterNames.PBES2_SALT_INPUT);
        Base64Url b = new Base64Url();
        byte[] saltInput = b.base64UrlDecode(saltInputString);
        assertTrue(saltInput.length >= MINIMUM_SALT_BYTE_LENGTH);
    }

    @Test
    public void testUsingAndSettingDefaults() throws JoseException
    {
        Pbes2HmacShaWithAesKeyWrapAlgorithm pbes2 = new Pbes2HmacShaWithAesKeyWrapAlgorithm.HmacSha256Aes128();

        assertTrue(pbes2.getDefaultIterationCount() >= MINIMUM_ITERAION_COUNT);
        assertTrue(pbes2.getDefaultSaltByteLength() >= MINIMUM_SALT_BYTE_LENGTH);

        PbkdfKey key = new PbkdfKey("a password");

        Headers headers = new Headers();
        Key derivedKey = pbes2.deriveForEncrypt(key, headers);
        assertThat(derivedKey.getEncoded().length, equalTo(16));

        String saltInputString = headers.getStringHeaderValue(HeaderParameterNames.PBES2_SALT_INPUT);
        byte[] saltInput = Base64Url.decode(saltInputString);
        assertThat(saltInput.length, equalTo(pbes2.getDefaultSaltByteLength()));
        Long iterationCount = headers.getLongHeaderValue(HeaderParameterNames.PBES2_ITERATION_COUNT);
        assertThat(iterationCount, equalTo(pbes2.getDefaultIterationCount()));

        Pbes2HmacShaWithAesKeyWrapAlgorithm newPbes2 = new Pbes2HmacShaWithAesKeyWrapAlgorithm.HmacSha256Aes128();
        long newDefaultIterationCount = 1024;
        newPbes2.setDefaultIterationCount(newDefaultIterationCount);

        int newDefaultSaltByteLength = 16;
        newPbes2.setDefaultSaltByteLength(newDefaultSaltByteLength);

        headers = new Headers();
        derivedKey = newPbes2.deriveForEncrypt(key, headers);
        saltInputString = headers.getStringHeaderValue(HeaderParameterNames.PBES2_SALT_INPUT);
        saltInput = Base64Url.decode(saltInputString);
        assertThat(saltInput.length, equalTo(newDefaultSaltByteLength));
        iterationCount = headers.getLongHeaderValue(HeaderParameterNames.PBES2_ITERATION_COUNT);
        assertThat(iterationCount, equalTo(newDefaultIterationCount));

        assertThat(derivedKey.getEncoded().length, equalTo(16));
    }

    @Test
    public void testSettingSaltAndIterationCount() throws JoseException
    {
        String password = "secret word";
        String plaintext = "<insert some witty quote or remark here, again>";

        JsonWebEncryption encryptingJwe  = new JsonWebEncryption();
        int saltByteLength = 32;
        String saltInputString = Base64Url.encode(ByteUtil.randomBytes(saltByteLength));
        encryptingJwe.getHeaders().setStringHeaderValue(HeaderParameterNames.PBES2_SALT_INPUT, saltInputString);
        long iterationCount = 1024L;
        encryptingJwe.getHeaders().setObjectHeaderValue(HeaderParameterNames.PBES2_ITERATION_COUNT, iterationCount);

        encryptingJwe.setAlgorithmHeaderValue(PBES2_HS384_A192KW);
        encryptingJwe.setEncryptionMethodHeaderParameter(AES_192_CBC_HMAC_SHA_384);
        encryptingJwe.setPayload(plaintext);
        encryptingJwe.setKey(new PbkdfKey(password));
        String compactSerialization = encryptingJwe.getCompactSerialization();

        JsonWebEncryption decryptingJwe = new JsonWebEncryption();
        decryptingJwe.setCompactSerialization(compactSerialization);
        decryptingJwe.setKey(new PbkdfKey(password));
        assertThat(plaintext, equalTo(decryptingJwe.getPayload()));

        String saltInputStringFromHeader = decryptingJwe.getHeader(HeaderParameterNames.PBES2_SALT_INPUT);
        assertThat(saltInputString, equalTo(saltInputStringFromHeader));
        assertThat(saltByteLength, equalTo(Base64Url.decode(saltInputStringFromHeader).length));
        long iterationCountFromHeader = decryptingJwe.getHeaders().getLongHeaderValue(HeaderParameterNames.PBES2_ITERATION_COUNT);
        assertThat(iterationCount, equalTo(iterationCountFromHeader));
    }


}
