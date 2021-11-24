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

import org.jose4j.jwa.Algorithm;
import org.jose4j.lang.InvalidKeyException;
import org.jose4j.lang.JoseException;

import java.security.Key;

/**
 */
public interface JsonWebSignatureAlgorithm extends Algorithm
{
    boolean verifySignature(byte[] signatureBytes, Key key, byte[] securedInputBytes) throws JoseException;
    byte[] sign(Key key, byte[] securedInputBytes) throws JoseException ;
    void validateSigningKey(Key key) throws InvalidKeyException;
    void validateVerificationKey(Key key) throws InvalidKeyException;
}
