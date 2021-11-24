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

package org.jose4j.keys;

import org.jose4j.lang.JoseException;

import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;

/**
 */
public class EcKeyUtil extends KeyPairUtil
{
    public static final String EC = "EC";

    @Override
    String getAlgorithm()
    {
        return EC;
    }

    public ECPublicKey publicKey(BigInteger x, BigInteger y, ECParameterSpec spec) throws JoseException
    {
        ECPoint w = new ECPoint(x, y);
        ECPublicKeySpec ecPublicKeySpec = new ECPublicKeySpec(w, spec);

        try
        {
            PublicKey publicKey = getKeyFactory().generatePublic(ecPublicKeySpec);
            return (ECPublicKey) publicKey;
        }
        catch (InvalidKeySpecException e)
        {
            throw new JoseException("Invalid key spec: " + e, e);
        }
    }

    public ECPrivateKey privateKey(BigInteger d, ECParameterSpec spec) throws JoseException
    {
        ECPrivateKeySpec ecPrivateKeySpec = new ECPrivateKeySpec(d, spec);

        try
        {
            PrivateKey privateKey = getKeyFactory().generatePrivate(ecPrivateKeySpec);
            return (ECPrivateKey) privateKey;
        }
        catch (InvalidKeySpecException e)
        {
            throw new JoseException("Invalid key spec: " + e, e);
        }
    }

    public KeyPair generateKeyPair(ECParameterSpec spec) throws JoseException
    {
        KeyPairGenerator keyGenerator = getKeyPairGenerator();

        try
        {
            keyGenerator.initialize(spec);
            return keyGenerator.generateKeyPair();
        }
        catch (InvalidAlgorithmParameterException e)
        {
            throw new JoseException("Unable to create EC key pair with spec " + spec, e);
        }
    }
}
