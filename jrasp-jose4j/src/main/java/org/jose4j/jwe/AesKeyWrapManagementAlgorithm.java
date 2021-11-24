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

package org.jose4j.jwe;

import org.jose4j.jwa.AlgorithmAvailability;
import org.jose4j.jwx.KeyValidationSupport;
import org.jose4j.keys.AesKey;
import org.jose4j.keys.KeyPersuasion;
import org.jose4j.lang.InvalidKeyException;

import java.security.Key;

/**
 */
public class AesKeyWrapManagementAlgorithm extends WrappingKeyManagementAlgorithm
{
    int keyByteLength;

    public AesKeyWrapManagementAlgorithm(String alg, int keyByteLength)
    {
        super("AESWrap", alg);
        setKeyType(AesKey.ALGORITHM);
        setKeyPersuasion(KeyPersuasion.SYMMETRIC);
        this.keyByteLength = keyByteLength;
    }

    int getKeyByteLength()
    {
        return keyByteLength;
    }

    @Override
    public void validateEncryptionKey(Key managementKey, ContentEncryptionAlgorithm contentEncryptionAlg) throws InvalidKeyException
    {
        validateKey(managementKey);
    }

    @Override
    public void validateDecryptionKey(Key managementKey, ContentEncryptionAlgorithm contentEncryptionAlg) throws InvalidKeyException
    {
        validateKey(managementKey);
    }

    void validateKey(Key managementKey) throws InvalidKeyException
    {
        KeyValidationSupport.validateAesWrappingKey(managementKey, getAlgorithmIdentifier(), getKeyByteLength());
    }

    @Override
    public boolean isAvailable()
    {
        int aesByteKeyLength = getKeyByteLength();
        String agl = getJavaAlgorithm();
        return AlgorithmAvailability.isAvailable("Cipher", agl) && CipherStrengthSupport.isAvailable(agl, aesByteKeyLength);
    }

    public static class Aes128 extends AesKeyWrapManagementAlgorithm
    {
        public Aes128()
        {
            super(KeyManagementAlgorithmIdentifiers.A128KW, 16);
        }
    }

    public static class Aes192 extends AesKeyWrapManagementAlgorithm
    {
        public Aes192()
        {
            super(KeyManagementAlgorithmIdentifiers.A192KW, 24);
        }
    }

    public static class Aes256 extends AesKeyWrapManagementAlgorithm
    {
        public Aes256()
        {
            super(KeyManagementAlgorithmIdentifiers.A256KW, 32);
        }
    }
}
