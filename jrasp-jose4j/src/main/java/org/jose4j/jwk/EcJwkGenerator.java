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

import org.jose4j.keys.EcKeyUtil;
import org.jose4j.lang.JoseException;

import java.security.KeyPair;
import java.security.PublicKey;
import java.security.spec.ECParameterSpec;

/**
 */
public class EcJwkGenerator
{
    public static EllipticCurveJsonWebKey generateJwk(ECParameterSpec spec) throws JoseException
    {
        EcKeyUtil keyUtil = new EcKeyUtil();
        KeyPair keyPair = keyUtil.generateKeyPair(spec);
        PublicKey publicKey = keyPair.getPublic();
        EllipticCurveJsonWebKey ecJwk = (EllipticCurveJsonWebKey) PublicJsonWebKey.Factory.newPublicJwk(publicKey);
        ecJwk.setPrivateKey(keyPair.getPrivate());
        return ecJwk;
    }
}
