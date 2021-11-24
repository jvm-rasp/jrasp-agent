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

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jose4j.keys.HmacKey;
import org.jose4j.lang.InvalidKeyException;
import org.jose4j.lang.JoseException;

import java.security.Key;

/**
 *
 */
public class HmacShaTest extends TestCase
{
    Log log = LogFactory.getLog(this.getClass());

    Key KEY1 = new HmacKey(new byte[]{-41, -1, 60, 1, 1, 45, -92, -114, 8, -1, -60, 7, 54, -16, 16, 14, -20, -85, 56,
            103, 4, 10, -56, 120, 37, -48, 6, 9, 110, -96, 27, -4, 41, -99, 60, 91, 49, 70, -99, -14, -108, -81, 60,
            37, 104, -116, 106, 104, -2, -95, 56, 103, 64, 10, -56, 120, 37, -48, 6, 9, 110, -96, 27, -4});
    Key KEY2 = new HmacKey(new byte[]{-67, 34, -45, 50, 13, 84, -79, 124, -16, -44, 26, -39, 4, -1, 26, 9, 38, 78,
            -107, 39, -81, 75, -18, 38, 96, 34, 13, 79, -73, 62, -60, 52, 71, -99, 60, 91, 124, 70, -9, -14, -108,
            -104, 6, 7, 104, -116, 6, 64, -2, -95, 56, 103, 64, 10, -56, 120, 37, -48, 6, 9, 110, -92, 27, -4});

    public void testHmacSha256A() throws JoseException
    {
        testBasicRoundTrip("some content that is the payload", AlgorithmIdentifiers.HMAC_SHA256);
    }

    public void testHmacSha256B() throws JoseException
    {
        testBasicRoundTrip("{\"iss\":\"https://jwt-idp.example.com\",\n" +
                "    \"prn\":\"mailto:mike@example.com\",\n" +
                "    \"aud\":\"https://jwt-rp.example.net\",\n" +
                "    \"iat\":1300815780,\n" +
                "    \"exp\":1300819380,\n" +
                "    \"http://claims.example.com/member\":true}", AlgorithmIdentifiers.HMAC_SHA256);
    }

    public void testHmacSha384A() throws JoseException
    {
        testBasicRoundTrip("Looking good, Billy Ray!", AlgorithmIdentifiers.HMAC_SHA384);
    }

    public void testHmacSha348B() throws JoseException
    {
        testBasicRoundTrip("{\"meh\":\"meh\"}", AlgorithmIdentifiers.HMAC_SHA384);
    }

    public void testHmacSha512A() throws JoseException
    {
        testBasicRoundTrip("Feeling good, Louis!", AlgorithmIdentifiers.HMAC_SHA512);
    }

    public void testHmacSha512B() throws JoseException
    {
        testBasicRoundTrip("{\"meh\":\"mehvalue\"}", AlgorithmIdentifiers.HMAC_SHA512);
    }

    void testBasicRoundTrip(String payload, String jwsAlgo) throws JoseException
    {
        JwsTestSupport.testBasicRoundTrip(payload, jwsAlgo, KEY1, KEY1, KEY2, KEY2);
    }

    public void testMinKeySize256ForSign()
    {
        JwsTestSupport.testBadKeyOnSign(AlgorithmIdentifiers.HMAC_SHA256, new HmacKey(new byte[1]));
    }

    public void testMinKeySize256ForSign2()
    {
        JwsTestSupport.testBadKeyOnSign(AlgorithmIdentifiers.HMAC_SHA256, new HmacKey(new byte[31]));
    }

    public void testMinKeySize384ForSign()
    {
        JwsTestSupport.testBadKeyOnSign(AlgorithmIdentifiers.HMAC_SHA384, new HmacKey(new byte[47]));
    }

    public void testMinKeySize512ForSign()
    {
        JwsTestSupport.testBadKeyOnSign(AlgorithmIdentifiers.HMAC_SHA512, new HmacKey(new byte[63]));
    }

    public void testMinKeySize256ForVerify() throws JoseException
    {
        String compactSerialization = "eyJhbGciOiJIUzI1NiJ9.c29tZSBjb250ZW50IHRoYXQgaXMgdGhlIHBheWxvYWQ.qGO7O7W2ECVl6uO7lfsXDgEF-EUEti0i-a_AimulIRA";
        Key key = new HmacKey(new byte[31]);
        JwsTestSupport.testBadKeyOnVerify(compactSerialization, key);
    }

    public void testMinKeySize384ForVerify() throws JoseException
    {
        String compactSerialization = "eyJhbGciOiJIUzM4NCJ9.eyJtZWgiOiJtZWgifQ.fptKQJmGN3fBP_FiQzdAGdmx-Q5iWjQvJrLfdmFnebxbQuzOmzejBrzYh4MyS01a";
        Key key = new HmacKey(new byte[47]);
        JwsTestSupport.testBadKeyOnVerify(compactSerialization, key);
    }

    public void testMinKeySize512ForVerify() throws JoseException
    {
        String compactSerialization = "eyJhbGciOiJIUzUxMiJ9.eyJtZWgiOiJtZWh2YWx1ZSJ9.NeB669dYkPmqgLqgd_sVqwIfCvb4XN-K67gpMJR93wfw_DylpxB1ell2opHM-E5P9jNKE2GYxTxwcI68Z2CTxw";
        Key key = new HmacKey(new byte[63]);
        JwsTestSupport.testBadKeyOnVerify(compactSerialization, key);
    }

    public void testVailidateKeySwitch() throws JoseException
    {
        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload("whatever");
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256);
        jws.setKey(new HmacKey(new byte[] {1,2,5,-9,99,-99,0,40,21}));
        jws.setDoKeyValidation(false);
        String cs = jws.getCompactSerialization();
        assertNotNull(cs);

        try
        {
            jws.setDoKeyValidation(true);
            jws.getCompactSerialization();
            Assert.fail("Should have failed with some kind of invalid key message but got " + cs);
        }
        catch (InvalidKeyException e)
        {
            log.debug("Expected something like this: " + e);
        }
    }

}
