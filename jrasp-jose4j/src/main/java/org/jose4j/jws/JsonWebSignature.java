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

import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwa.AlgorithmFactory;
import org.jose4j.jwa.AlgorithmFactoryFactory;
import org.jose4j.jwx.CompactSerializer;
import org.jose4j.jwx.HeaderParameterNames;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.keys.KeyPersuasion;
import org.jose4j.lang.IntegrityException;
import org.jose4j.lang.InvalidAlgorithmException;
import org.jose4j.lang.JoseException;
import org.jose4j.lang.StringUtil;

import java.security.Key;

/**
 */
public class JsonWebSignature extends JsonWebStructure
{
    public static final short COMPACT_SERIALIZATION_PARTS = 3;

    private String payload;
    private String payloadCharEncoding = StringUtil.UTF_8;

    private Boolean validSignature;

    public JsonWebSignature()
    {
        setAlgorithmConstraints(AlgorithmConstraints.DISALLOW_NONE);
    }

    public void setPayload(String payload)
    {
        this.payload = payload;
    }

    protected void setCompactSerializationParts(String[] parts) throws JoseException
    {
        if (parts.length != COMPACT_SERIALIZATION_PARTS)
        {
            throw new JoseException("A JWS Compact Serialization must have exactly "+COMPACT_SERIALIZATION_PARTS+" parts separated by period ('.') characters");
        }

        setEncodedHeader(parts[0]);
        setEncodedPayload(parts[1]);
        setSignature(base64url.base64UrlDecode(parts[2]));
    }


    public String getCompactSerialization() throws JoseException
    {
        this.sign();
        return CompactSerializer.serialize(getSigningInput(), getEncodedSignature());
    }

    public void sign() throws JoseException
    {
        JsonWebSignatureAlgorithm algorithm = getAlgorithm();
        Key signingKey = getKey();
        if (isDoKeyValidation())
        {
            algorithm.validateSigningKey(signingKey);
        }
        byte[] inputBytes = getSigningInputBytes();
        byte[] signatureBytes = algorithm.sign(signingKey, inputBytes);
        setSignature(signatureBytes);
    }

    public boolean verifySignature() throws JoseException
    {
        if (validSignature == null)
        {
            JsonWebSignatureAlgorithm algorithm = getAlgorithm();
            Key verificationKey = getKey();
            if (isDoKeyValidation())
            {
                algorithm.validateVerificationKey(verificationKey);
            }
            byte[] signatureBytes = getSignature();
            byte[] inputBytes = getSigningInputBytes();
            validSignature = algorithm.verifySignature(signatureBytes, verificationKey, inputBytes);
        }

        return validSignature;
    }

    public JsonWebSignatureAlgorithm getAlgorithm() throws InvalidAlgorithmException
    {
        String algo = getAlgorithmHeaderValue();
        if (algo == null)
        {
            throw new InvalidAlgorithmException("Signature algorithm header ("+HeaderParameterNames.ALGORITHM+") not set.");
        }

        getAlgorithmConstraints().checkConstraint(algo);
        AlgorithmFactoryFactory factoryFactory = AlgorithmFactoryFactory.getInstance();
        AlgorithmFactory<JsonWebSignatureAlgorithm> jwsAlgorithmFactory = factoryFactory.getJwsAlgorithmFactory();
        return jwsAlgorithmFactory.getAlgorithm(algo);
    }

    private byte[] getSigningInputBytes() throws JoseException
    {
        String signingInput = getSigningInput();
        return StringUtil.getBytesAscii(signingInput);
    }

    private String getSigningInput() throws JoseException
    {
        return CompactSerializer.serialize(getEncodedHeader(), getEncodedPayload());
    }

    public String getPayload() throws JoseException
    {
        if (!verifySignature())
        {
            throw new IntegrityException("JWS signature is invalid.");
        }
        return payload;
    }

    public String getUnverifiedPayload()
    {
        return payload;
    }

    public String getPayloadCharEncoding()
    {
        return payloadCharEncoding;
    }

    public void setPayloadCharEncoding(String payloadCharEncoding)
    {
        this.payloadCharEncoding = payloadCharEncoding;
    }

    public String getKeyType() throws InvalidAlgorithmException
    {
        return getAlgorithm().getKeyType();
    }

    public KeyPersuasion getKeyPersuasion() throws InvalidAlgorithmException
    {
        return getAlgorithm().getKeyPersuasion();
    }

    public void setEncodedPayload(String encodedPayload)
    {
        setPayload(base64url.base64UrlDecodeToString(encodedPayload, payloadCharEncoding));
    }

    public String getEncodedPayload()
    {
        return base64url.base64UrlEncode(payload, getPayloadCharEncoding());
    }

    public String getEncodedSignature()
    {
        return base64url.base64UrlEncode(getSignature());
    }

    protected byte[] getSignature()
    {
        return getIntegrity();
    }

    protected void setSignature(byte[] signature)
    {
        setIntegrity(signature);
    }
}
