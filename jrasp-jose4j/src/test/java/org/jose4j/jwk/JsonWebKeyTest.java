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

import junit.framework.TestCase;
import org.jose4j.keys.ExampleEcKeysFromJws;
import org.jose4j.keys.ExampleRsaKeyFromJws;
import org.jose4j.lang.JoseException;

import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;

/**
 */
public class JsonWebKeyTest extends TestCase
{
    public void testFactoryWithRsaPublicKey() throws JoseException
    {
        JsonWebKey jwk = JsonWebKey.Factory.newJwk(ExampleRsaKeyFromJws.PUBLIC_KEY);
        assertIsRsa(jwk);
    }

    private void assertIsRsa(JsonWebKey jwk)
    {
        assertTrue(jwk instanceof RsaJsonWebKey);
        assertTrue(jwk.getKey() instanceof RSAPublicKey);
        assertEquals(RsaJsonWebKey.KEY_TYPE, jwk.getKeyType());
    }

    public void testFactoryWithEcPublicKey() throws JoseException
    {
        JsonWebKey jwk = JsonWebKey.Factory.newJwk(ExampleEcKeysFromJws.PUBLIC_256);
        assertIsEllipticCurve(jwk);
    }

    private void assertIsEllipticCurve(JsonWebKey jwk)
    {
        assertTrue(jwk.getKey() instanceof ECPublicKey);
        assertTrue(jwk instanceof EllipticCurveJsonWebKey);
        assertEquals(EllipticCurveJsonWebKey.KEY_TYPE, jwk.getKeyType());
    }

    public void testEcSingleJwkToAndFromJson() throws JoseException
    {
        String jwkJson =
                "       {\"kty\":\"EC\",\n" +
                "        \"crv\":\"P-256\",\n" +
                "        \"x\":\"MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4\",\n" +
                "        \"y\":\"4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM\",\n" +
                "        \"use\":\"enc\",\n" +
                "        \"kid\":\"1\"}";

        JsonWebKey jwk = JsonWebKey.Factory.newJwk(jwkJson);
        assertIsEllipticCurve(jwk);

        String jsonOut = jwk.toJson();
        JsonWebKey jwk2 = JsonWebKey.Factory.newJwk(jsonOut);
        assertIsEllipticCurve(jwk2);
    }

    public void testRsaSingleJwkToAndFromJson() throws JoseException
    {
        String jwkJson =
                  "       {\"kty\":\"RSA\",\n" +
                "        \"n\": \"0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx" +
                "   4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMs" +
                "   tn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2" +
                "   QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbI" +
                "   SD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniIqb" +
                "   w0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw\",\n" +
                "        \"e\":\"AQAB\",\n" +
                "        \"alg\":\"RS256\"}";

        JsonWebKey jwk = JsonWebKey.Factory.newJwk(jwkJson);
        assertIsRsa(jwk);

        String jsonOut = jwk.toJson();
        JsonWebKey jwk2 = JsonWebKey.Factory.newJwk(jsonOut);
        assertIsRsa(jwk2); 
    }
}
