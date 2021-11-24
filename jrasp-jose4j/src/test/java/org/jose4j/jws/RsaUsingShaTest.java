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

import junit.framework.TestCase;
import org.jose4j.jwx.KeyValidationSupport;
import org.jose4j.keys.ExampleEcKeysFromJws;
import org.jose4j.keys.ExampleRsaKeyFromJws;
import org.jose4j.keys.HmacKey;
import org.jose4j.keys.RsaKeyUtil;
import org.jose4j.lang.JoseException;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 */
public class RsaUsingShaTest extends TestCase
{
    public void testRoundTrips() throws JoseException
    {
        RsaKeyUtil keyUtil = new RsaKeyUtil();
        KeyPair pair = keyUtil.generateKeyPair(KeyValidationSupport.MIN_RSA_KEY_LENGTH);

        PrivateKey priv1 = pair.getPrivate();
        PublicKey pub1 = pair.getPublic();
        PrivateKey priv2 = ExampleRsaKeyFromJws.PRIVATE_KEY;
        PublicKey pub2 = ExampleRsaKeyFromJws.PUBLIC_KEY;
        JwsTestSupport.testBasicRoundTrip("PAYLOAD!!!", AlgorithmIdentifiers.RSA_USING_SHA256, priv1, pub1, priv2, pub2);
        JwsTestSupport.testBasicRoundTrip("PAYLOAD!!", AlgorithmIdentifiers.RSA_USING_SHA384, priv1, pub1, priv2, pub2);
        JwsTestSupport.testBasicRoundTrip("PAYLOAD!", AlgorithmIdentifiers.RSA_USING_SHA512, priv1, pub1, priv2, pub2);
    }

    public void testBadKeys() throws JoseException
    {
        RsaKeyUtil keyUtil = new RsaKeyUtil();

        KeyPair pair = keyUtil.generateKeyPair(1024);
        PublicKey pub = pair.getPublic();
        PrivateKey priv = pair.getPrivate();

        String cs256 = "eyJhbGciOiJSUzI1NiJ9.UEFZTE9BRCEhIQ.ln8y7TlxyR0jLemqdVybaWYmcS2nIseDEqKNJ1J-mM6TXRWjfFKsJr1kzBgh1nKHbVT6q_cgSoPLsb-9WGvpUMkt7N0NxqT2Vffcz_2HMwKvWDJZSjbuj6_XHSJye7gqySHiI2gOggSaYyIqnua-_kOmVGmgncrzwm2YRPgwLXAl9zB0GNul7lNGDvs193WbgOJ-rKGj515NBfqb7cV2VjQg7vsrnzIWT8FKcrQ5TYNXMrybzK5Q_1BNIxOVlrTsdh_pcUNiJvKKgC3_5PBHkhaJrJlxfwmi77YW8ezwXpFKdzbh8cKKzO0ZhamOOJns99HPPot4jr26JCERzBVF3g";
        String cs384 = "eyJhbGciOiJSUzM4NCJ9.UEFZTE9BRCEhIQ.E27QWhxodHU2vB-C3eKr4SQR8YF1jptmDrw7LRtQF1105bUk_WQqI8dCZcJDBsHdJ11O7JEmnRPJLiZd50eFnzcvZsAN5gh7q2eNnxCPuXjH2MoyRlIt6-8aSs-Es0l66Sz4slyOGjqRBRBqHcr7bu6gjo7mBh3XzS8ORnu5zn9Gj5XWr3emX5vwTq66UCfkyf6a2aa4knmYbGW0JiELVWU4rU2UhY5NjhxDW4omlOGiLpNhaX3LAgvA5nvNLi8HFlhVG8-GO4malIjj6rFdpwpZXm3G-sMbpWCcNyu3DUxRDKgjIWjX2SpGLqgXYZEMcAjmF2CA3tsxy43aUalMYQ";
        String cs512 = "eyJhbGciOiJSUzUxMiJ9.UEFZTE9BRCEhIQ.d7n7w-Ndg1-zRrAAQ3kgP_3vg70M5YcPS4eVrGTgD3UILRnMz5rBQh4k42yTVC53K-pmA6ZpphVtlC0lI7j2ViOM9ObC-dR_vOCN0_X7wo3D8qY5KJUDacMpDb_YkWtc5aUpaLilCe7770vNuOU6GK4hXkbTALJuug1V87QVn-xKDHAGMx_b2UgkzybbnribIAeMoqsgg5P9hCSu63xd8OxagbMzPC46ovr5IvTAhIJuONYeGQaOSdOMFFvuZzsZVmdwTQfC9zv-oC3vIF3BcSd1y_8b7CNlFw2NdIf0G3whEnrZgIYofKjZ3QkrIMRGzEF4H3u3KxVwdgpc1OhVSQ";
        for (String cs : new String[] {cs256, cs384, cs512})
        {
            JwsTestSupport.testBadKeyOnVerify(cs, pub);
            JwsTestSupport.testBadKeyOnVerify(cs, priv);
            JwsTestSupport.testBadKeyOnVerify(cs, ExampleRsaKeyFromJws.PRIVATE_KEY);
            JwsTestSupport.testBadKeyOnVerify(cs, null);
            JwsTestSupport.testBadKeyOnVerify(cs, new HmacKey(new byte[2048]));
            JwsTestSupport.testBadKeyOnVerify(cs, ExampleEcKeysFromJws.PUBLIC_256);
            JwsTestSupport.testBadKeyOnVerify(cs, ExampleEcKeysFromJws.PUBLIC_521);
            JwsTestSupport.testBadKeyOnVerify(cs, ExampleEcKeysFromJws.PRIVATE_256);
            JwsTestSupport.testBadKeyOnVerify(cs, ExampleEcKeysFromJws.PRIVATE_521);
        }
    }
}