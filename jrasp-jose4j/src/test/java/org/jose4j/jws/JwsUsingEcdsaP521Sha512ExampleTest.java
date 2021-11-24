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
import org.jose4j.keys.ExampleEcKeysFromJws;
import org.jose4j.lang.JoseException;

/**
 */
public class JwsUsingEcdsaP521Sha512ExampleTest extends TestCase
{
    String JWS = "eyJhbGciOiJFUzUxMiJ9.UGF5bG9hZA.AdwMgeerwtHoh-l192l60hp9wAHZFVJbLfD_UxMi70cwnZOYaRI1bKPWROc-mZZqwqT2SI-KGDKB34XO0aw_7XdtAG8GaSwFKdCAPZgoXD2YBJZCPEX3xKpRwcdOO8KpEHwJjyqOgzDO7iKvU8vcnwNrmxYbSW9ERBXukOXolLzeO_Jn";

    public void testVerifyExample() throws JoseException
    {
        JsonWebSignature jws = new JsonWebSignature();
        jws.setCompactSerialization(JWS);
        jws.setKey(ExampleEcKeysFromJws.PUBLIC_521);
        assertTrue("signature should validate", jws.verifySignature());
    }

    public void testVerifyExampleFromDraft14() throws JoseException
    {
        String jwsCs = "eyJhbGciOiJFUzUxMiJ9" +
                "." +
//                "eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFt" +
//                "cGxlLmNvbS9pc19yb290Ijp0cnVlfQ" +
                "UGF5bG9hZA" +
                "." +
                "AdwMgeerwtHoh-l192l60hp9wAHZFVJbLfD_UxMi70cwnZOYaRI1bKPWROc-mZZq" +
                "wqT2SI-KGDKB34XO0aw_7XdtAG8GaSwFKdCAPZgoXD2YBJZCPEX3xKpRwcdOO8Kp" +
                "EHwJjyqOgzDO7iKvU8vcnwNrmxYbSW9ERBXukOXolLzeO_Jn";

        String jwkJson = "     {\"kty\":\"EC\",\n" +
                "      \"crv\":\"P-521\",\n" +
                "      \"x\":\"AekpBQ8ST8a8VcfVOTNl353vSrDCLLJXmPk06wTjxrrjcBpXp5EOnYG_\n" +
                "           NjFZ6OvLFV1jSfS9tsz4qUxcWceqwQGk\",\n" +
                "      \"y\":\"ADSmRA43Z1DSNx_RvcLI87cdL07l6jQyyBXMoxVg_l2Th-x3S1WDhjDl\n" +
                "           y79ajL4Kkd0AZMaZmh9ubmf63e3kyMj2\",\n" +
                "      \"d\":\"AY5pb7A0UFiB3RELSD64fTLOSV_jazdF7fLYyuTw8lOfRhWg6Y6rUrPA\n" +
                "           xerEzgdRhajnu0ferB0d53vM9mE15j2C\"\n" +
                "     }";

        JsonWebKey jwk = JsonWebKey.Factory.newJwk(jwkJson);

        JsonWebSignature jws = new JsonWebSignature();
        jws.setCompactSerialization(jwsCs);
        jws.setKey(jwk.getKey());
        String payload = jws.getPayload();
        System.out.println(payload);
        assertTrue("signature should validate", jws.verifySignature());
    }

}
