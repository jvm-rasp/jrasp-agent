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
import org.jose4j.keys.BigEndianBigInteger;
import org.jose4j.keys.X509Util;
import org.jose4j.lang.JoseException;
import org.jose4j.lang.JsonHelp;

import java.math.BigInteger;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 */
public abstract class PublicJsonWebKey extends JsonWebKey
{

    public static final String X509_CERTIFICATE_CHAIN_PARAMETER = "x5c";

    // todo x5others
    public static final String X509_THUMBPRINT_PARAMETER = "x5t";
    public static final String X509_URL_PARAMETER = "x5u";

    protected boolean writeOutPrivateKeyToJson;
    protected PrivateKey privateKey;

    private List<X509Certificate> certificateChain;

    protected PublicJsonWebKey(PublicKey publicKey)
    {
        super(publicKey);
    }

    protected PublicJsonWebKey(Map<String, Object> params) throws JoseException
    {
        super(params);

        if (params.containsKey(X509_CERTIFICATE_CHAIN_PARAMETER))
        {
            List<String> x5cStrings = JsonHelp.getStringArray(params, X509_CERTIFICATE_CHAIN_PARAMETER);
            certificateChain = new ArrayList<X509Certificate>(x5cStrings.size());

            X509Util x509Util = new X509Util();

            for (String b64EncodedDer : x5cStrings)
            {
                X509Certificate x509Certificate = x509Util.fromBase64Der(b64EncodedDer);
                certificateChain.add(x509Certificate);
            }
        }
    }

    protected abstract void fillPublicTypeSpecificParams(Map<String,Object> params);
    protected abstract void fillPrivateTypeSpecificParams(Map<String,Object> params);

    protected void fillTypeSpecificParams(Map<String,Object> params, OutputControlLevel outputLevel)
    {
        fillPublicTypeSpecificParams(params);

        if (certificateChain != null)
        {
            X509Util x509Util = new X509Util();
            List<String> x5cStrings = new ArrayList<String>(certificateChain.size());

            for (X509Certificate cert : certificateChain)
            {
               String b64EncodedDer = x509Util.toBase64Der(cert);
               x5cStrings.add(b64EncodedDer);
            }

            params.put(X509_CERTIFICATE_CHAIN_PARAMETER, x5cStrings);
        }

        if (writeOutPrivateKeyToJson || outputLevel == OutputControlLevel.INCLUDE_PRIVATE)
        {
            fillPrivateTypeSpecificParams(params);
        }
    }

    public PublicKey getPublicKey()
    {
        return (PublicKey) key;
    }

    /**
     * @deprecated as of 0.3.2 use {@link #toJson(org.jose4j.jwk.JsonWebKey.OutputControlLevel)}
     */
    public void setWriteOutPrivateKeyToJson(boolean writeOutPrivateKeyToJson)
    {
        this.writeOutPrivateKeyToJson = writeOutPrivateKeyToJson;
    }

    public PrivateKey getPrivateKey()
    {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey)
    {
        this.privateKey = privateKey;
    }

    public List<X509Certificate> getCertificateChain()
    {
        return certificateChain;
    }

    public X509Certificate getLeafCertificate()
    {
        return (certificateChain != null && !certificateChain.isEmpty()) ? certificateChain.get(0) : null;
    }

    public void setCertificateChain(List<X509Certificate> certificateChain)
    {
        checkForBareKeyCertMismatch();

        this.certificateChain = certificateChain;
    }

    void checkForBareKeyCertMismatch()
    {
        X509Certificate leafCertificate = getLeafCertificate();
        boolean certAndBareKeyMismatch = leafCertificate != null && !leafCertificate.getPublicKey().equals(getPublicKey());
        if (certAndBareKeyMismatch)
        {
            throw new IllegalArgumentException( "The key in the first certificate MUST match the bare public key " +
                "represented by other members of the JWK. Public key = " + getPublicKey() + " cert = " + leafCertificate);
        }
    }

    public void setCertificateChain(X509Certificate... certificates)
    {
        setCertificateChain(Arrays.asList(certificates));
    }

    BigInteger getBigIntFromBase64UrlEncodedParam(Map<String, Object> params, String parameterName)
    {
        String base64UrlValue = JsonHelp.getString(params, parameterName);
        return BigEndianBigInteger.fromBase64Url(base64UrlValue);
    }

    void putBigIntAsBase64UrlEncodedParam(Map<String,Object> params, String parameterName, BigInteger value)
    {
        String base64UrlValue = BigEndianBigInteger.toBase64Url(value);
        params.put(parameterName, base64UrlValue);
    }

    void putBigIntAsBase64UrlEncodedParam(Map<String,Object> params, String parameterName, BigInteger value, int minLength)
    {
        String base64UrlValue = BigEndianBigInteger.toBase64Url(value, minLength);
        params.put(parameterName, base64UrlValue);
    }

    public static class Factory
    {
        public static PublicJsonWebKey newPublicJwk(Map<String,Object> params) throws JoseException
        {
            JsonWebKey jsonWebKey = JsonWebKey.Factory.newJwk(params);
            return (PublicJsonWebKey) jsonWebKey;
        }

        public static PublicJsonWebKey newPublicJwk(Key publicKey) throws JoseException
        {
            JsonWebKey jsonWebKey = JsonWebKey.Factory.newJwk(publicKey);
            return (PublicJsonWebKey) jsonWebKey;
        }

        public static PublicJsonWebKey newPublicJwk(String json) throws JoseException
        {
            Map<String, Object> parsed = JsonUtil.parseJson(json);
            return newPublicJwk(parsed);
        }
    }
}
