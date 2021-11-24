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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

/**
 */
public class RsaKeyUtil extends KeyPairUtil
{
    public static final String RSA = "RSA";

    @Override
    String getAlgorithm()
    {
        return RSA;
    }

    public RSAPublicKey publicKey(BigInteger modulus, BigInteger publicExponent) throws JoseException
    {
        RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(modulus, publicExponent);
        try
        {
            PublicKey publicKey = getKeyFactory().generatePublic(rsaPublicKeySpec);
            return (RSAPublicKey) publicKey;
        }
        catch (InvalidKeySpecException e)
        {
            throw new JoseException("Invalid key spec: " + e, e);
        }
    }

    public RSAPrivateKey privateKey(BigInteger modulus, BigInteger privateExponent) throws JoseException
    {
        RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(modulus, privateExponent);
        return getRsaPrivateKey(keySpec);
    }

    public RSAPrivateKey privateKey(BigInteger modulus, BigInteger publicExponent, BigInteger privateExponent, BigInteger primeP,
                                    BigInteger primeQ, BigInteger primeExponentP, BigInteger primeExponentQ,
                                    BigInteger crtCoefficient) throws JoseException
    {
        RSAPrivateCrtKeySpec keySpec = new RSAPrivateCrtKeySpec(modulus,
                publicExponent, privateExponent, primeP, primeQ, primeExponentP, primeExponentQ, crtCoefficient);
        return getRsaPrivateKey(keySpec);
    }

    public RSAPrivateKey getRsaPrivateKey(RSAPrivateKeySpec keySpec) throws JoseException
    {
        try
        {
            PrivateKey privateKey = getKeyFactory().generatePrivate(keySpec);
            return (RSAPrivateKey) privateKey;
        }
        catch (InvalidKeySpecException e)
        {
            throw new JoseException("Invalid key spec: " + e, e);
        }
    }

    public KeyPair generateKeyPair(int bits) throws JoseException
    {
        KeyPairGenerator keyGenerator = getKeyPairGenerator();
        keyGenerator.initialize(bits);
        return keyGenerator.generateKeyPair();
    }
}
