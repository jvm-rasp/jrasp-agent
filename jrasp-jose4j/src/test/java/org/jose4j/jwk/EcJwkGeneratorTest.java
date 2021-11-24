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
import org.jose4j.keys.EllipticCurves;
import org.jose4j.lang.JoseException;

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;

/**
 */
public class EcJwkGeneratorTest extends TestCase
{
    public void testGen() throws JoseException
    {
        for (ECParameterSpec spec : new ECParameterSpec[]{EllipticCurves.P256, EllipticCurves.P384, EllipticCurves.P521})
        {
            EllipticCurveJsonWebKey ecJwk = EcJwkGenerator.generateJwk(spec);
            assertNotNull(ecJwk.getKey());
            assertTrue(ecJwk.getKey() instanceof ECPublicKey);
            assertNotNull(ecJwk.getPublicKey());
            assertTrue(ecJwk.getPublicKey() instanceof ECPublicKey);
            assertNotNull(ecJwk.getPrivateKey());
            assertTrue(ecJwk.getPrivateKey() instanceof ECPrivateKey);
        }
    }
}
