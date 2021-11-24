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

package org.jose4j.jwx;

import org.jose4j.base64url.Base64Url;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;

import java.security.Key;

/**
 */
public abstract class JsonWebStructure
{
    protected Base64Url base64url = new Base64Url();

    protected Headers headers = new Headers();

    private byte[] integrity;

    private Key key;

    protected boolean doKeyValidation = true;

    private AlgorithmConstraints algorithmConstraints = AlgorithmConstraints.NO_CONSTRAINTS;

    abstract public String getCompactSerialization() throws JoseException;
    abstract protected void setCompactSerializationParts(String[] parts) throws JoseException;

    abstract public String getPayload() throws JoseException;
    abstract public void setPayload(String payload);

    public static JsonWebStructure fromCompactSerialization(String cs) throws JoseException
    {
        JsonWebStructure jsonWebObject;
        String[] parts = CompactSerializer.deserialize(cs);
        if (parts.length == JsonWebEncryption.COMPACT_SERIALIZATION_PARTS)
        {
            jsonWebObject = new JsonWebEncryption();
        }
        else if (parts.length == JsonWebSignature.COMPACT_SERIALIZATION_PARTS)
        {
            jsonWebObject = new JsonWebSignature();
        }
        else
        {
            throw new JoseException("Invalid JOSE Compact Serialization. Expecting either "
                    + JsonWebSignature.COMPACT_SERIALIZATION_PARTS + " or "
                    + JsonWebEncryption.COMPACT_SERIALIZATION_PARTS
                    + " parts for JWS or JWE respectively but was " + parts.length + ".");
        }

        jsonWebObject.setCompactSerializationParts(parts);
        return jsonWebObject;
    }

    public void setCompactSerialization(String compactSerialization) throws JoseException
    {
    	String[] parts = CompactSerializer.deserialize(compactSerialization);
        setCompactSerializationParts(parts);
    }

    /**
     * @deprecated replaced by {@link #getHeaders()} and {@link org.jose4j.jwx.Headers#getFullHeaderAsJsonString()}
     */
    public String getHeader()
    {
        return getHeaders().getFullHeaderAsJsonString();
    }

    protected String getEncodedHeader()
    {
        return headers.getEncodedHeader();
    }

    public void setHeader(String name, String value)
    {
        headers.setStringHeaderValue(name, value);
    }

    protected void setEncodedHeader(String encodedHeader) throws JoseException
    {
        checkNotEmptyPart(encodedHeader, "Encoded Header");
        headers.setEncodedHeader(encodedHeader);
    }

    public Headers getHeaders()
    {
        return headers;
    }

    protected void checkNotEmptyPart(String encodedPart, String partName) throws JoseException
    {
        if (encodedPart == null || encodedPart.length() == 0)
        {
            throw new JoseException("The "+ partName +" cannot be empty.");
        }
    }

    public String getHeader(String name)
    {
        return headers.getStringHeaderValue(name);
    }

    public void setAlgorithmHeaderValue(String alg)
    {
        setHeader(HeaderParameterNames.ALGORITHM, alg);
    }

    public String getAlgorithmHeaderValue()
    {
        return getHeader(HeaderParameterNames.ALGORITHM);
    }

    public void setKeyIdHeaderValue(String kid)
    {
        setHeader(HeaderParameterNames.KEY_ID, kid);
    }

    public String getKeyIdHeaderValue()
    {
        return getHeader(HeaderParameterNames.KEY_ID);
    }

    public Key getKey()
    {
        return key;
    }

    public void setKey(Key key)
    {
        this.key = key;
    }

    protected byte[] getIntegrity()
    {
        return integrity;
    }

    protected void setIntegrity(byte[] integrity)
    {
        this.integrity = integrity;
    }

    public boolean isDoKeyValidation()
    {
        return doKeyValidation;
    }

    public void setDoKeyValidation(boolean doKeyValidation)
    {
        this.doKeyValidation = doKeyValidation;
    }

    protected AlgorithmConstraints getAlgorithmConstraints()
    {
        return algorithmConstraints;
    }

    public void setAlgorithmConstraints(AlgorithmConstraints algorithmConstraints)
    {
        this.algorithmConstraints = algorithmConstraints;
    }
}
