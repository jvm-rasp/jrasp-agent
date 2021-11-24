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

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 */
public class RsaJwkGeneratorTest extends TestCase
{
    public void testGenerateJwk() throws Exception
    {
        RsaJsonWebKey rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048);
        assertNotNull(rsaJsonWebKey.getPrivateKey());
        assertTrue(rsaJsonWebKey.getKey() instanceof RSAPublicKey);
        assertNotNull(rsaJsonWebKey.getPublicKey());
        assertTrue(rsaJsonWebKey.getPublicKey() instanceof RSAPublicKey);
        assertNotNull(rsaJsonWebKey.getPrivateKey());
        assertTrue(rsaJsonWebKey.getPrivateKey() instanceof RSAPrivateKey);
    }
}
