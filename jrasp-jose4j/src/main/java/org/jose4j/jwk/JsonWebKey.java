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

import org.jose4j.json.JsonUtil;
import org.jose4j.lang.JoseException;
import org.jose4j.lang.JsonHelp;

import java.io.Serializable;
import java.security.Key;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 */
public abstract class JsonWebKey implements Serializable
{
    public enum OutputControlLevel {INCLUDE_PRIVATE, INCLUDE_SYMMETRIC, PUBLIC_ONLY}

    public static final String KEY_TYPE_PARAMETER = "kty";
    public static final String USE_PARAMETER = "use";
    public static final String KEY_ID_PARAMETER = "kid";
    public static final String ALGORITHM_PARAMETER = "alg";

    private String use;
    private String keyId;
    private String algorithm;

    protected Key key;

    protected JsonWebKey(Key key)
    {
        this.key = key;
    }

    protected JsonWebKey(Map<String, Object> params)
    {
        setUse(JsonHelp.getString(params, USE_PARAMETER));
        setKeyId(JsonHelp.getString(params, KEY_ID_PARAMETER));
        setAlgorithm(JsonHelp.getString(params, ALGORITHM_PARAMETER));
    }

    public abstract String getKeyType();
    protected abstract void fillTypeSpecificParams(Map<String,Object> params, OutputControlLevel outputLevel);

    /**
     * @deprecated deprecated in favor {@link #getKey()} or {@link PublicJsonWebKey#getPublicKey()}
     */
    public PublicKey getPublicKey()
    {
        try
        {
            return (PublicKey) key;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public Key getKey()
    {
        return key;
    }

    public String getUse()
    {
        return use;
    }

    public void setUse(String use)
    {
        this.use = use;
    }

    public String getKeyId()
    {
        return keyId;
    }

    public void setKeyId(String keyId)
    {
        this.keyId = keyId;
    }

    public String getAlgorithm()
    {
        return algorithm;
    }

    public void setAlgorithm(String algorithm)
    {
        this.algorithm = algorithm;
    }

    public Map<String, Object> toParams(OutputControlLevel outputLevel)
    {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put(KEY_TYPE_PARAMETER, getKeyType());
        putIfNotNull(KEY_ID_PARAMETER, getKeyId(), params);
        putIfNotNull(USE_PARAMETER, getUse(), params);
        putIfNotNull(ALGORITHM_PARAMETER, getAlgorithm(), params);
        fillTypeSpecificParams(params, outputLevel);
        return params;
    }

    public String toJson()
    {
        return toJson(OutputControlLevel.INCLUDE_SYMMETRIC);
    }

    public String toJson(OutputControlLevel outputLevel)
    {
        Map<String, Object> params = toParams(outputLevel);
        return JsonUtil.toJson(params);
    }

    @Override
    public String toString()
    {
        return getClass().getName() + toParams(OutputControlLevel.PUBLIC_ONLY);
    }

    protected void putIfNotNull(String name, String value, Map<String, Object> params)
    {
        if (value != null)
        {
            params.put(name,value);
        }
    }

    public static class Factory
    {
        public static JsonWebKey newJwk(Map<String,Object> params) throws JoseException
        {
            String kty = JsonHelp.getString(params, KEY_TYPE_PARAMETER);

            if (RsaJsonWebKey.KEY_TYPE.equals(kty))
            {
                return new RsaJsonWebKey(params);
            }
            else if (EllipticCurveJsonWebKey.KEY_TYPE.equals(kty))
            {
                return new EllipticCurveJsonWebKey(params);
            }
            else if (OctetSequenceJsonWebKey.KEY_TYPE.equals(kty))
            {
                return new OctetSequenceJsonWebKey(params);
            }
            else
            {
                throw new JoseException("Unknown key algorithm: '" + kty + "'");
            }
        }

        public static JsonWebKey newJwk(Key key) throws JoseException
        {
            if (RSAPublicKey.class.isInstance(key))
            {
                return new RsaJsonWebKey((RSAPublicKey)key);
            }
            else if (ECPublicKey.class.isInstance(key))
            {
                return new EllipticCurveJsonWebKey((ECPublicKey)key);
            }
            else if (PublicKey.class.isInstance(key))
            {
                throw new JoseException("Unsupported or unknown public key " + key);
            }
            else
            {
                return new OctetSequenceJsonWebKey(key);
            }
        }

        public static JsonWebKey newJwk(String json) throws JoseException
        {
            Map<String, Object> parsed = JsonUtil.parseJson(json);
            return newJwk(parsed);
        }
    }
}
