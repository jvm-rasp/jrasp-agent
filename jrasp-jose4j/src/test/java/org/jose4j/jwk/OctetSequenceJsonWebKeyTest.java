/*
 * Copyright 2012-2013 Brian Campbell
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

package org.jose4j.jwk;

import org.hamcrest.CoreMatchers;
import org.jose4j.keys.AesKey;
import org.jose4j.keys.HmacKey;
import org.jose4j.lang.ByteUtil;
import org.jose4j.lang.JoseException;
import org.junit.Assert;
import org.junit.Test;

import java.security.Key;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.*;
import static org.jose4j.jwk.JsonWebKey.OutputControlLevel.*;
import static org.junit.Assert.*;

/**
 */
public class OctetSequenceJsonWebKeyTest
{
    @Test
    public void testExampleFromJws() throws Exception
    {
        String base64UrlKey = "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow";
        String jwkJson ="{\"kty\":\"oct\",\n"+" \"k\":\""+base64UrlKey+"\"\n"+"}";
        JsonWebKey parsedKey = JsonWebKey.Factory.newJwk(jwkJson);
        assertEquals(OctetSequenceJsonWebKey.class, parsedKey.getClass());

        // these octets are from an earlier draft version (pre -12 I think) before JWKs were
        // used to encode the example keys. makes for a nice test though
        int[]  keyInts = {3, 35, 53, 75, 43, 15, 165, 188, 131, 126, 6, 101, 119, 123, 166,
                           143, 90, 179, 40, 230, 240, 84, 201, 40, 169, 15, 132, 178, 210, 80,
                           46, 191, 211, 251, 90, 146, 210, 6, 71, 239, 150, 138, 180, 195, 119,
                           98, 61, 34, 61, 46, 33, 114, 5, 46, 79, 8, 192, 205, 154, 245, 103,
                           208, 128, 163};
        byte[] keyBytes = ByteUtil.convertUnsignedToSignedTwosComp(keyInts);
        assertTrue(Arrays.equals(keyBytes, parsedKey.getKey().getEncoded()));

        JsonWebKey jwk = JsonWebKey.Factory.newJwk(new HmacKey(keyBytes));

        assertEquals(OctetSequenceJsonWebKey.KEY_TYPE, jwk.getKeyType());
        assertTrue(jwk.toJson().contains(base64UrlKey));
        assertTrue(jwk.toJson(INCLUDE_PRIVATE).contains(base64UrlKey));
        assertTrue(jwk.toJson(INCLUDE_SYMMETRIC).contains(base64UrlKey));
        assertFalse(jwk.toJson(PUBLIC_ONLY).contains(base64UrlKey));
    }

    @Test
    public void testLeadingAndTrailingZeros() throws JoseException
    {
        byte[] rawInputBytes = new byte[] {0,0,111,16,51,98,-4,0,-72,9,-111,60,41,-66,94,0};
        JsonWebKey jwk = JsonWebKey.Factory.newJwk(new AesKey(rawInputBytes));
        String json = jwk.toJson(INCLUDE_SYMMETRIC);

        JsonWebKey jwkFromJson = JsonWebKey.Factory.newJwk(json);
        byte[] encoded = jwkFromJson.getKey().getEncoded();
        assertThat(rawInputBytes.length, is(equalTo(encoded.length)));
        assertArrayEquals(rawInputBytes, encoded);
    }
}
