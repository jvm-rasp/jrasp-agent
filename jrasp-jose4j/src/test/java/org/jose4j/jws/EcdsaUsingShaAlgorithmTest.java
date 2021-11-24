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

package org.jose4j.jws;

import org.jose4j.base64url.Base64Url;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.lang.ByteUtil;
import org.jose4j.lang.JoseException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.*;

/**
 */
public class EcdsaUsingShaAlgorithmTest
{
    @Test
    public void testEncodingDecoding() throws IOException
    {
        // not sure this is a *useful* test but what the heck...
        int[] rints = {14, 209, 33, 83, 121, 99, 108, 72, 60, 47, 127, 21, 88,
            7, 212, 2, 163, 178, 40, 3, 58, 249, 124, 126, 23, 129,
            154, 195, 22, 158, 166, 101};
        
        int[] sints =  {197, 10, 7, 211, 140, 60, 112, 229, 216, 241, 45, 175,
            8, 74, 84, 128, 166, 101, 144, 197, 242, 147, 80, 154,
            143, 63, 127, 138, 131, 163, 84, 213};

        byte[] rbytes = ByteUtil.convertUnsignedToSignedTwosComp(rints);
        byte[] sbytes = ByteUtil.convertUnsignedToSignedTwosComp(sints);

        int capacity = 64;
        ByteBuffer buffer = ByteBuffer.allocate(capacity);
        buffer.put(rbytes);
        buffer.put(sbytes);

        byte[] concatedBytes = buffer.array();
        byte[] derEncoded = EcdsaUsingShaAlgorithm.convertConcatenatedToDer(concatedBytes);
        Assert.assertFalse(Arrays.equals(concatedBytes, derEncoded));
        byte[] backToConcated = EcdsaUsingShaAlgorithm.convertDerToConcatenated(derEncoded, capacity);

        Assert.assertTrue(Arrays.equals(concatedBytes, backToConcated));
    }

    @Test
    public void simpleConcatenationWithLength() throws IOException
    {
        byte[] noPad = {1, 2};
        byte[] der = EcdsaUsingShaAlgorithm.convertConcatenatedToDer(noPad);

        int outputLength = 16;
        byte[] concatenated = EcdsaUsingShaAlgorithm.convertDerToConcatenated(der, outputLength);

        Assert.assertThat(outputLength, is(equalTo(concatenated.length)));
        Assert.assertThat(concatenated[7], is(equalTo(noPad[0])));
        Assert.assertThat(concatenated[15], is(equalTo(noPad[1])));

        System.out.println(Arrays.toString(concatenated));
    }

    @Test
    public void simpleConcatenationWithDiffLengths() throws IOException
    {
        byte[] a = new byte[] {0,0,0,0,1,1,1,1};
        byte[] b = new byte[] {2,2,2,2,2,2,2,2};
        byte[] der = EcdsaUsingShaAlgorithm.convertConcatenatedToDer(ByteUtil.concat(a, b));

        int outputLength = 16;
        byte[] concatenated = EcdsaUsingShaAlgorithm.convertDerToConcatenated(der, outputLength);
        Assert.assertThat(outputLength, is(equalTo(concatenated.length)));

        int halfLength = outputLength / 2;
        byte[] first = ByteUtil.subArray(concatenated, 0, halfLength);
        byte[] second = ByteUtil.subArray(concatenated, halfLength, halfLength);
        Assert.assertArrayEquals(a, first);
        Assert.assertArrayEquals(b, second);
    }

    @Test
    public void simpleConcatenationWithVeryDiffLengths() throws IOException
    {
        byte[] a = new byte[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1};
        byte[] b = new byte[] {2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2};
        byte[] der = EcdsaUsingShaAlgorithm.convertConcatenatedToDer(ByteUtil.concat(a, b));

        int outputLength = 32;
        byte[] concatenated = EcdsaUsingShaAlgorithm.convertDerToConcatenated(der, outputLength);
        Assert.assertThat(outputLength, is(equalTo(concatenated.length)));

        int halfLength = outputLength / 2;
        byte[] first = ByteUtil.subArray(concatenated, 0, halfLength);
        byte[] second = ByteUtil.subArray(concatenated, halfLength, halfLength);
        Assert.assertArrayEquals(a, first);
        Assert.assertArrayEquals(b, second);
    }

    @Test
    public void tooShortPreviously() throws Exception
    {
        // a ECDSA 521 sig value produced before jose4j left zero padded the R & S values
        String encoded = "7w6JjwMqcWmTFaZfrOc5kSSj5WOi0vDbMoGqcLWUL5QrTmJ_KOPMkNOjNll4pRITxuyZo_owOswnDM4dYdS7ypo" +
                "PHOL13XDfdffG7sdwjXA6JthsItlk6l43Xtqt2ytJKqUMC-J7K5Cn1izOeuqzsI18Go9jcEEw5eUdQhR77OjfCA";

        byte[] decoded = Base64Url.decode(encoded);
        byte[] der = EcdsaUsingShaAlgorithm.convertConcatenatedToDer(decoded);
        int outputLength = 132;
        byte[] concatenated = EcdsaUsingShaAlgorithm.convertDerToConcatenated(der, outputLength);
        Assert.assertThat(outputLength, is(equalTo(concatenated.length)));
        Assert.assertThat((byte) 0, is(equalTo(concatenated[0])));
        Assert.assertThat((byte) 0, is(equalTo(concatenated[66])));
    }

    @Test
    public void backwardCompatibility() throws JoseException
    {
        /*
            Strictly speaking these are all invalid because the length of the signature value is 130 bytes rather
            than the 132 that it should be. However, prior to 0.3.5, jose4j was not left zero padding the values
            in the ECDSA signature and I want to still accept such signatures so as to allow for interoperability
            between versions. The JWS compact serializations here were all produced with the buggy code from 0.3.4
            and this test checks that jose4j can still validate the signatures.
         */

        String jwk = "{\"kty\":\"EC\"," +
                "\"x\":\"APlFpj7M-Kj8ArsYMbJS-6rn1XkugUwngk_iTVe_KfLs6pVIb4LYz-gJ2SytwsoNkSbwq6NuNXB3kFsiYXmG0pf2\"," +
                "\"y\":\"AebLEK2Hn_vLyDFCzQYGBrGF7eJPh2b01vZ_rK1UOXT9slDvNFK5y6yUSkG4qrVg5P0xwuw25AReYwtvwYQr8uvV\"," +
                "\"crv\":\"P-521\"," +
                "\"d\":\"AL-txDgStuoyYEJ3-NyMNeTjlwcoQxbck659Snelqza-Vhd166l3Bfh4A0o42DqetfknQBeE-upPEliNEtEvv9dN\"}";
        String cs = "eyJhbGciOiJFUzUxMiJ9." +
                "ZG9lcyBpdCBtYXR0ZXIgd2hhdCdzIGluIGhlcmU_IEkgZG9uOyd0IGtub3cuLi4." +
                "zv6B3bm8xz6EKfQaaW-0sVVD7MYoym-cXrq2SaDGI9_EZkP244jQk1xtyX6uK8JlSXXRlYR7WJ2rCM8NOr_ZHB5b7VaJnOnJkzR" +
                "nh3-ncI46Dhj-cbqsVqZvvylkWDxhoodVkhAPT2wnkbfS6mYHjmYzWI1YF2ub5klAunLjn8jFdg";
        check(jwk, cs);

        jwk = "{\"kty\":\"EC\"," +
                "\"x\":\"ACDqsfERDEacSJUa-3M2TxIp05yVHl5yuURP0WhZvi4xfMiRsyqooEWhA9PtHEko1ELvaM0bR0hNavo597HtP5_q\"," +
                "\"y\":\"AW90m8N4e9YUwYG-Yxkf5T2rR5fiECj-A0p1DVUJNJ8BFPr5OGG1z3GO_PMxC-7LCj8gfqr6Wc8a1ViqIt6OE8Nr\"," +
                "\"crv\":\"P-521\"," +
                "\"d\":\"AGS5ZSjsn_ou9mqkutgJAUKz5Hx7XATfHvNTUv_1CAHN08LVBU_1R2TEtJanWe72w3d22ylwHTPoogAbRQdhTyYC\"}\n";
        cs = "eyJhbGciOiJFUzUxMiJ9." +
                "ZG9lcyBpdCBtYXR0ZXIgd2hhdCdzIGluIGhlcmU_IEkgZG9uOyd0IGtub3cuLi4." +
                "k-m9qenb1rrmhpavhQ6PeklKRXn7Tu7J9Asycgj4gUELLTGHE96Di5_euQF0avKkVrorDuDdtzi-q0hnzq38ArKTpbkjRqdMonQ" +
                "dhFTXroP6HCkSrlSWFUTxvtsoaa-VorugOxPe1wZSHafmaWotbqDJ2jXA3sSC1H3jVxx1SxXGRg";

        check(jwk, cs);

        jwk =  "{\"kty\":\"EC\"," +
                "\"x\":\"AQ8WdkBzMgfuWCWvGIpGkyi-DZgw4a1wmTZVg9YjUzSUj8NKLDcYnUgsr4op7z8dW8WUib6dC4EGXISaye1Svp6S\"," +
                "\"y\":\"AMr47PiklLy_Py-QgB1jOsoVlbujFwDuM6vdTorColeNVWw2FQi-oUN-Pt8ga9mD1LDgAC96lTSybpgTu9G1P_ir\"," +
                "\"crv\":\"P-521\"," +
                "\"d\":\"AaDOIsjeA20NpIDcQN6yBZ-I1XEOQSsolqsZBSWllmNjVfefggm-Erjz4UdWrgKVdZNlD5px3i5L30dhWZc-45kC\"}\n";
        cs = "eyJhbGciOiJFUzUxMiJ9." +
                "ZG9lcyBpdCBtYXR0ZXIgd2hhdCdzIGluIGhlcmU_IEkgZG9uOyd0IGtub3cuLi4." +
                "waSI2xpnm4zQeAyyRLDmoq5nf_tj9SoSxLvXWcYhpNX56UVM3PyyCkX5aIzGH25kJ-W-10QzF-tR8PoIHxlNEMgfJFGHW4Bje" +
                "xe-juNyvnETJbDyipP_i4t0wuUIVJ1J43ihHvLhXiWgfivNjwfVikMC3mTWdyzUxwrjG4M0XaUC-w";
        check(jwk, cs);
    }

    private void check(String jwkJson, String cs) throws JoseException
    {
        JsonWebKey jwk = JsonWebKey.Factory.newJwk(jwkJson);
        JsonWebSignature jws = new JsonWebSignature();
        jws.setCompactSerialization(cs);
        jws.setKey(jwk.getKey());
        Assert.assertTrue(jws.verifySignature());
    }

}
