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

import org.jose4j.base64url.Base64;
import org.jose4j.lang.JoseException;

import java.io.ByteArrayInputStream;
import java.security.cert.*;

/**
 */
public class X509Util
{
    private static final String FACTORY_TYPE = "X.509";

    private CertificateFactory certFactory;


    public X509Util()
    {
        try
        {
            certFactory = CertificateFactory.getInstance(FACTORY_TYPE);
        }
        catch (CertificateException e)
        {
            throw new IllegalStateException("Couldn't find "+ FACTORY_TYPE + " CertificateFactory!?!", e);
        }
    }

    public String toBase64Der(X509Certificate x509Certificate)
    {
        try
        {
            byte[] der = x509Certificate.getEncoded();
            return Base64.encode(der);
        }
        catch (CertificateEncodingException e)
        {
            throw new IllegalStateException("Unexpected problem getting encoded certificate.", e);
        }
    }

    public X509Certificate fromBase64Der(String b64EncodedDer) throws JoseException
    {
        byte[] der = Base64.decode(b64EncodedDer);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(der);
        try
        {
            Certificate certificate = certFactory.generateCertificate(byteArrayInputStream);
            return (X509Certificate) certificate;
        }
        catch (CertificateException e)
        {
            throw new JoseException("Unable to convert " + b64EncodedDer + " value to X509Certificate: " + e, e);
        }
    }


}
