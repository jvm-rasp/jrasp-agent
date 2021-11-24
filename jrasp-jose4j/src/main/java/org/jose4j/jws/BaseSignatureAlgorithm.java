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
import org.jose4j.keys.KeyPersuasion;
import org.jose4j.lang.InvalidKeyException;
import org.jose4j.lang.JoseException;

import java.security.*;
import java.security.spec.AlgorithmParameterSpec;

/**
 */
public abstract class BaseSignatureAlgorithm extends AlgorithmInfo implements JsonWebSignatureAlgorithm
{
    private AlgorithmParameterSpec algorithmParameterSpec;

    public BaseSignatureAlgorithm(String id, String javaAlgo, String keyAlgo)
    {
        setAlgorithmIdentifier(id);
        setJavaAlgorithm(javaAlgo);
        setKeyPersuasion(KeyPersuasion.ASYMMETRIC);
        setKeyType(keyAlgo);
    }

    protected void setAlgorithmParameterSpec(AlgorithmParameterSpec algorithmParameterSpec)
    {
        this.algorithmParameterSpec = algorithmParameterSpec;
    }

    public boolean verifySignature(byte[] signatureBytes, Key key, byte[] securedInputBytes) throws JoseException
    {
        Signature signature = getSignature();
        initForVerify(signature, key);
        try
        {
            signature.update(securedInputBytes);
            return signature.verify(signatureBytes);
        }
        catch (SignatureException e)
        {
            throw new JoseException("Problem verifying signature.", e);
        }
    }

    public byte[] sign(Key key, byte[] securedInputBytes) throws JoseException
    {
        Signature signature = getSignature();
        initForSign(signature, key);
        try
        {
            signature.update(securedInputBytes);
            return signature.sign();
        }
        catch (SignatureException e)
        {
            throw new JoseException("Problem creating signature.", e);
        }
    }

    private void initForSign(Signature signature, Key key) throws InvalidKeyException
    {
        try
        {
            PrivateKey privateKey = (PrivateKey) key;
            signature.initSign(privateKey);
        }
        catch (java.security.InvalidKeyException e)
        {
            throw new InvalidKeyException(getBadKeyMessage(key) + "for " + getJavaAlgorithm(), e);
        }
    }

    private void initForVerify(Signature signature, Key key) throws InvalidKeyException
    {
        try
        {
           PublicKey publicKey = (PublicKey) key;
           signature.initVerify(publicKey);
        }
        catch (java.security.InvalidKeyException e)
        {
            throw new InvalidKeyException(getBadKeyMessage(key) + "for " + getJavaAlgorithm(), e);
        }
    }

    private String getBadKeyMessage(Key key)
    {
        String msg = key == null ? "key is null" : "algorithm=" + key.getAlgorithm();
        return "The given key (" + msg + ") is not valid ";
    }

    private Signature getSignature() throws JoseException
    {

        try
        {
            Signature signature = Signature.getInstance(getJavaAlgorithm());
            if (algorithmParameterSpec != null)
            {
                signature.setParameter(algorithmParameterSpec);
            }
            return signature;
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new JoseException("Unable to get an implementation of algorithm name: " + getJavaAlgorithm(), e);
        }
        catch (InvalidAlgorithmParameterException e)
        {
            throw new JoseException("Invalid algorithm parameter ("+algorithmParameterSpec+") for: " + getJavaAlgorithm(), e);
        }
    }

    public abstract void validatePrivateKey(PrivateKey privateKey) throws InvalidKeyException;

    public void validateSigningKey(Key key) throws InvalidKeyException
    {
        checkForNullKey(key);

        try
        {
            validatePrivateKey((PrivateKey)key);
        }
        catch (ClassCastException e)
        {
            throw new InvalidKeyException(getBadKeyMessage(key) + "(not a private key or is the wrong type of key) for "
                    + getJavaAlgorithm() + " / " + getAlgorithmIdentifier() + " " +  e);
        }
    }

    public abstract void validatePublicKey(PublicKey publicKey) throws InvalidKeyException;

    public void validateVerificationKey(Key key) throws InvalidKeyException
    {
        checkForNullKey(key);

        try
        {
            validatePublicKey((PublicKey)key);
        }
        catch (ClassCastException e)
        {
            throw new InvalidKeyException(getBadKeyMessage(key) + "(not a public key or is the wrong type of key) for "
                    + getJavaAlgorithm() + "/" + getAlgorithmIdentifier() + " " +  e);
        }
    }

    private void checkForNullKey(Key key) throws InvalidKeyException
    {
        if (key == null)
        {
            throw new InvalidKeyException("Key cannot be null");
        }
    }

    @Override
    public boolean isAvailable()
    {
        try
        {
            Signature signature = getSignature();
            return signature != null;
        }
        catch (JoseException e)
        {
            log.debug(getAlgorithmIdentifier() + " vai " + getJavaAlgorithm() +
                    " is NOT available from the underlying JCE (" + e + ").");
            return false;

        }
    }
}
