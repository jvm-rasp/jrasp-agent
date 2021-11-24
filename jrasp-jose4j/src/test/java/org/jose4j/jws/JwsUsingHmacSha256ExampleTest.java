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

import junit.framework.TestCase;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.lang.JoseException;


/**
 */
public class JwsUsingHmacSha256ExampleTest extends TestCase
{
    String JWS = "eyJ0eXAiOiJKV1QiLA0KICJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    String PAYLOAD = "{\"iss\":\"joe\",\r\n \"exp\":1300819380,\r\n \"http://example.com/is_root\":true}";
    String JWK = "{\"kty\":\"oct\",\"k\":\"AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow\"}";

    public void testVerifyExample() throws JoseException
    {
        JsonWebSignature jws = new JsonWebSignature();
        jws.setCompactSerialization(JWS);
        JsonWebKey jsonWebKey = JsonWebKey.Factory.newJwk(JWK);
        jws.setKey(jsonWebKey.getKey());
        assertTrue("signature (HMAC) should validate", jws.verifySignature());
        assertEquals(PAYLOAD, jws.getPayload());
    }

    public void testSignExample() throws JoseException
    {
        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(PAYLOAD);

        JsonWebKey jsonWebKey = JsonWebKey.Factory.newJwk(JWK);
        jws.setKey(jsonWebKey.getKey());
        jws.getHeaders().setFullHeaderAsJsonString("{\"typ\":\"JWT\",\r\n \"alg\":\"HS256\"}");

        String compactSerialization = jws.getCompactSerialization();

        assertEquals("example jws value doesn't match calculated compact serialization", JWS, compactSerialization);
    }

}
