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

package org.jose4j.jwx;

import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.IntegrityException;
import org.jose4j.lang.JoseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 */
public class JsonWebStructureTest
{
    private static final String YOU_LL_GET_NOTHING_AND_LIKE_IT = "You'll get nothing, and like it!";

    private JsonWebKey oct256bitJwk;

    @Before
    public void symmetricJwk() throws JoseException
    {
        String json = "{\"kty\":\"oct\",\"kid\":\"9er\",\"k\":\"Ul3CckPpDfGjBzSsXCoQSvX3L0qVcAku2hW9WU-ccSs\"}";
        oct256bitJwk = JsonWebKey.Factory.newJwk(json);
    }

    @Test
    public void jws1() throws JoseException
    {
        String cs = "eyJhbGciOiJIUzI1NiIsImtpZCI6IjllciJ9." +
                "WW91J2xsIGdldCBub3RoaW5nLCBhbmQgbGlrZSBpdCE." +
                "45s_xV_ol7JBwVcTPbWbaYT5i4mb7j27lEhi_bxpExw";
        JsonWebStructure jwx = JsonWebStructure.fromCompactSerialization(cs);
        Assert.assertTrue(cs + " should give a JWS " + jwx, jwx instanceof JsonWebSignature);
        Assert.assertEquals(AlgorithmIdentifiers.HMAC_SHA256, jwx.getAlgorithmHeaderValue());
        jwx.setKey(oct256bitJwk.getKey());
        String payload = jwx.getPayload();
        Assert.assertEquals(YOU_LL_GET_NOTHING_AND_LIKE_IT, payload);
        Assert.assertEquals(oct256bitJwk.getKeyId(), jwx.getKeyIdHeaderValue());
    }

    @Test (expected = IntegrityException.class)
    public void integrityCheckFailsJws() throws JoseException
    {
        String cs = "eyJhbGciOiJIUzI1NiIsImtpZCI6IjllciJ9." +
                "RGFubnksIEknbSBoYXZpbmcgYSBwYXJ0eSB0aGlzIHdlZWtlbmQuLi4gSG93IHdvdWxkIHlvdSBsaWtlIHRvIGNvbWUgb3ZlciBhbmQgbW93IG15IGxhd24_." +
                "45s_xV_ol7JBwVcTPbWbaYT5i4mb7j27lEhi_bxpExw";
        JsonWebStructure jwx = JsonWebStructure.fromCompactSerialization(cs);
        Assert.assertTrue(cs + " should give a JWS " + jwx, jwx instanceof JsonWebSignature);
        Assert.assertEquals(AlgorithmIdentifiers.HMAC_SHA256, jwx.getAlgorithmHeaderValue());
        jwx.setKey(oct256bitJwk.getKey());
        Assert.assertEquals(oct256bitJwk.getKeyId(), jwx.getKeyIdHeaderValue());
        jwx.getPayload();
    }

    @Test
    public void jwe1() throws JoseException
    {
        String cs = "eyJhbGciOiJkaXIiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2Iiwia2lkIjoiOWVyIn0." +
                "." +
                "XAog2l7TP5-0mIPYjT2ZYg." +
                "Zf6vQZhxeAfzk2AyuXsKJSo1R8aluPDvK7a6N7wvSmuIUczDhUtJFmNdXC3d4rPa." +
                "XBTguLfGeGKu6YsQVnes2w";
        JsonWebStructure jwx = JsonWebStructure.fromCompactSerialization(cs);
        jwx.setKey(oct256bitJwk.getKey());
        Assert.assertTrue(cs + " should give a JWE " + jwx, jwx instanceof JsonWebEncryption);
        Assert.assertEquals(KeyManagementAlgorithmIdentifiers.DIRECT, jwx.getAlgorithmHeaderValue());
        Assert.assertEquals(oct256bitJwk.getKeyId(), jwx.getKeyIdHeaderValue());
        String payload = jwx.getPayload();
        Assert.assertEquals(YOU_LL_GET_NOTHING_AND_LIKE_IT, payload);
    }

    @Test
    public void jwe2() throws JoseException
    {
        String cs = "eyJhbGciOiJBMjU2S1ciLCJlbmMiOiJBMTI4Q0JDLUhTMjU2Iiwia2lkIjoiOWVyIn0." +
                "RAqGCBMFk7O-B-glFckcFmxUr8BTTXuZk-bXAdRZxpk5Vgs_1yoUQw." +
                "hyl68_ADlK4VRDYiQMQS6w." +
                "xk--JKIVF4Xjxc0gRGPL30s4PSNtj685WYqXbjyItG0uSffD4ajGXdz4BO8i0sbM." +
                "WXaAVpBgftXyO1HkkRvgQQ";
        JsonWebStructure jwx = JsonWebStructure.fromCompactSerialization(cs);
        jwx.setKey(oct256bitJwk.getKey());
        Assert.assertTrue(cs + " should give a JWE " + jwx, jwx instanceof JsonWebEncryption);
        Assert.assertEquals(KeyManagementAlgorithmIdentifiers.A256KW, jwx.getAlgorithmHeaderValue());
        Assert.assertEquals(oct256bitJwk.getKeyId(), jwx.getKeyIdHeaderValue());
        String payload = jwx.getPayload();
        Assert.assertEquals(YOU_LL_GET_NOTHING_AND_LIKE_IT, payload);
    }

    @Test (expected = IntegrityException.class)
    public void integrityCheckFailsJwe() throws JoseException
    {
        String cs = "eyJhbGciOiJBMjU2S1ciLCJlbmMiOiJBMTI4Q0JDLUhTMjU2Iiwia2lkIjoiOWVyIn0." +
                "RAqGCBMFk7O-B-glFckcFmxUr8BTTXuZk-bXAdRZxpk5Vgs_1yoUQw." +
                "hyl68_ADlK4VRDYiQMQS6w." +
                "xk--JKIVF4Xjxc0gRGPL30s4PSNtj685WYqXbjyItG0uSffD4ajGXdz4BO8i0sbM." +
                "aXaAVpBgftxqO1HkkRvgab";
        JsonWebStructure jwx = JsonWebStructure.fromCompactSerialization(cs);
        jwx.setKey(oct256bitJwk.getKey());
        Assert.assertTrue(cs + " should give a JWE " + jwx, jwx instanceof JsonWebEncryption);
        Assert.assertEquals(KeyManagementAlgorithmIdentifiers.A256KW, jwx.getAlgorithmHeaderValue());
        Assert.assertEquals(oct256bitJwk.getKeyId(), jwx.getKeyIdHeaderValue());
        jwx.getPayload();
    }

    @Test (expected = JoseException.class)
    public void testFromInvalidCompactSerialization1() throws Exception
    {
        JsonWebStructure.fromCompactSerialization("blah.blah.blah.blah");
    }

    @Test (expected = JoseException.class)
    public void testFromInvalidCompactSerialization2() throws Exception
    {
        JsonWebStructure.fromCompactSerialization("nope");
    }

    @Test (expected = JoseException.class)
    public void testFromInvalidCompactSerialization3() throws Exception
    {
        JsonWebStructure.fromCompactSerialization("blah.blah.blah.blah.too.darn.many");
    }

    @Test (expected = JoseException.class)
    public void testFromInvalidCompactSerialization4() throws Exception
    {
        JsonWebStructure.fromCompactSerialization("eyJhbGciOiJIUzI1NiJ9." +
                "." +
                "c29tZSBjb250ZW50IHRoYXQgaXMgdGhlIHBheWxvYWQ." +
                "qGO7O7W2ECVl6uO7lfsXDgEF-EUEti0i-a_AimulIRA");
    }
}
