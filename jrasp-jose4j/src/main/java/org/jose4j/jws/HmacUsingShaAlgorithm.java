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

import org.jose4j.jwa.AlgorithmAvailability;
import org.jose4j.jwa.AlgorithmInfo;
import org.jose4j.keys.HmacKey;
import org.jose4j.keys.KeyPersuasion;
import org.jose4j.lang.ByteUtil;
import org.jose4j.lang.InvalidKeyException;
import org.jose4j.mac.MacUtil;

import javax.crypto.Mac;
import java.security.Key;

/**
 */
public class HmacUsingShaAlgorithm extends AlgorithmInfo implements JsonWebSignatureAlgorithm
{
    private int minimumKeyLength;

    public HmacUsingShaAlgorithm(String id, String javaAlgo, int minimumKeyLength)
    {
        setAlgorithmIdentifier(id);
        setJavaAlgorithm(javaAlgo);
        setKeyPersuasion(KeyPersuasion.SYMMETRIC);
        setKeyType(HmacKey.ALGORITHM);
        this.minimumKeyLength = minimumKeyLength;
    }

    public boolean verifySignature(byte[] signatureBytes, Key key, byte[] securedInputBytes) throws InvalidKeyException
    {
        Mac mac = getMacInstance(key);
        byte[] calculatedSigature = mac.doFinal(securedInputBytes);

        return ByteUtil.secureEquals(signatureBytes, calculatedSigature);
    }

    public byte[] sign(Key key, byte[] securedInputBytes) throws InvalidKeyException
    {
        Mac mac = getMacInstance(key);
        return mac.doFinal(securedInputBytes);
    }

    private Mac getMacInstance(Key key) throws InvalidKeyException
    {
        return MacUtil.getInitializedMac(getJavaAlgorithm(), key);
    }

    void validateKey(Key key) throws InvalidKeyException
    {
        int length = ByteUtil.bitLength(key.getEncoded());
        if (length < minimumKeyLength)
        {
            throw new InvalidKeyException("A key of the same size as the hash output (i.e. "+minimumKeyLength+
                    " bits for "+getAlgorithmIdentifier()+
                    ") or larger MUST be used with the HMAC SHA algorithms but this key is only " + length + " bits");
        }
    }

    public void validateSigningKey(Key key) throws InvalidKeyException
    {
        validateKey(key);
    }

    public void validateVerificationKey(Key key) throws InvalidKeyException
    {
        validateKey(key);
    }

    @Override
    public boolean isAvailable()
    {
        return AlgorithmAvailability.isAvailable("Mac", getJavaAlgorithm());
    }

    public static class HmacSha256 extends HmacUsingShaAlgorithm
    {
        public HmacSha256()
        {
            super(AlgorithmIdentifiers.HMAC_SHA256, MacUtil.HMAC_SHA256, 256);
        }
    }

    public static class HmacSha384 extends HmacUsingShaAlgorithm
    {
        public HmacSha384()
        {
            super(AlgorithmIdentifiers.HMAC_SHA384, MacUtil.HMAC_SHA384, 384);
        }
    }

    public static class HmacSha512 extends HmacUsingShaAlgorithm
    {
        public HmacSha512()
        {
            super(AlgorithmIdentifiers.HMAC_SHA512, MacUtil.HMAC_SHA512, 512);
        }
    }
}
