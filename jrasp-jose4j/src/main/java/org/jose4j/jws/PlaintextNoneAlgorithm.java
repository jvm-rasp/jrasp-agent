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

import org.jose4j.jwa.AlgorithmInfo;
import org.jose4j.jwx.HeaderParameterNames;
import org.jose4j.keys.KeyPersuasion;
import org.jose4j.lang.ByteUtil;
import org.jose4j.lang.InvalidKeyException;
import org.jose4j.lang.JoseException;

import java.security.Key;

/**
 */
public class PlaintextNoneAlgorithm extends AlgorithmInfo implements JsonWebSignatureAlgorithm
{
    private static final String CANNOT_HAVE_KEY_MESSAGE = "JWS Plaintext ("+ HeaderParameterNames.ALGORITHM+"="+ AlgorithmIdentifiers.NONE+") must not use a key.";

    public PlaintextNoneAlgorithm()
    {
        setAlgorithmIdentifier(AlgorithmIdentifiers.NONE);
        setKeyPersuasion(KeyPersuasion.NONE);
    }

    public boolean verifySignature(byte[] signatureBytes, Key key, byte[] securedInputBytes) throws JoseException
    {
        validateKey(key);

        return (signatureBytes.length == 0);
    }

    public byte[] sign(Key key, byte[] securedInputBytes) throws JoseException
    {
        validateKey(key);

        return ByteUtil.EMPTY_BYTES;
    }

    public void validateSigningKey(Key key) throws InvalidKeyException
    {
        validateKey(key);
    }

    public void validateVerificationKey(Key key) throws InvalidKeyException
    {
        validateKey(key);
    }

    private void validateKey(Key key) throws InvalidKeyException
    {
        if (key != null)
        {
            throw new InvalidKeyException(CANNOT_HAVE_KEY_MESSAGE);
        }
    }

    @Override
    public boolean isAvailable()
    {
        return true;
    }
}
