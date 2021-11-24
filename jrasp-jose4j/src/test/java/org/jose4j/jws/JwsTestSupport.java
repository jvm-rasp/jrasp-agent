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

import junit.framework.Assert;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jose4j.jwx.CompactSerializer;
import org.jose4j.lang.InvalidKeyException;
import org.jose4j.lang.JoseException;

import java.security.Key;

/**
 */
public class JwsTestSupport
{
    static Log log = LogFactory.getLog(JwsTestSupport.class);

    static void testBasicRoundTrip(String payload, String jwsAlgo, Key signingKey1, Key verificationKey1, Key signingKey2, Key verificationKey2) throws JoseException
    {
        JsonWebSignature jwsWithKey1 = new JsonWebSignature();
        jwsWithKey1.setPayload(payload);
        jwsWithKey1.setAlgorithmHeaderValue(jwsAlgo);
        jwsWithKey1.setKey(signingKey1);
        String serializationWithKey1 = jwsWithKey1.getCompactSerialization();

        log.debug(jwsAlgo + " " + serializationWithKey1);

        JsonWebSignature jwsWithKey2 = new JsonWebSignature();
        jwsWithKey2.setKey(signingKey2);
        jwsWithKey2.setAlgorithmHeaderValue(jwsAlgo);
        jwsWithKey2.setPayload(payload);
        String serializationWithKey2 = jwsWithKey2.getCompactSerialization();
        JwsTestSupport.validateBasicStructure(serializationWithKey1);
        JwsTestSupport.validateBasicStructure(serializationWithKey2);
        Assert.assertFalse(serializationWithKey1.equals(serializationWithKey2));

        JsonWebSignature jws = new JsonWebSignature();
        jws.setCompactSerialization(serializationWithKey1);
        jws.setKey(verificationKey1);
        Assert.assertTrue(jws.verifySignature());
        Assert.assertEquals(payload, jws.getPayload());

        jws = new JsonWebSignature();
        jws.setCompactSerialization(serializationWithKey2);
        jws.setKey(verificationKey1);
        Assert.assertFalse(jws.verifySignature());

        jws = new JsonWebSignature();
        jws.setCompactSerialization(serializationWithKey2);
        jws.setKey(verificationKey2);
        Assert.assertTrue(jws.verifySignature());
        Assert.assertEquals(payload, jws.getPayload());

        jws = new JsonWebSignature();
        jws.setCompactSerialization(serializationWithKey1);
        jws.setKey(verificationKey2);
        Assert.assertFalse(jws.verifySignature());

        Assert.assertEquals(payload, jwsWithKey1.getUnverifiedPayload());
        Assert.assertEquals(payload, jwsWithKey2.getUnverifiedPayload());
    }

    static void validateBasicStructure(String compactSerialization) throws JoseException
    {
        Assert.assertNotNull(compactSerialization);
        Assert.assertEquals(compactSerialization.trim(), compactSerialization);
        String[] parts = CompactSerializer.deserialize(compactSerialization);
        Assert.assertEquals(JsonWebSignature.COMPACT_SERIALIZATION_PARTS, parts.length);
    }

    static void testBadKeyOnSign(String alg, Key key)
    {
        try
        {
            JsonWebSignature jwsWithKey1 = new JsonWebSignature();
            jwsWithKey1.setPayload("whatever");
            jwsWithKey1.setAlgorithmHeaderValue(alg);
            jwsWithKey1.setKey(key);
            String cs = jwsWithKey1.getCompactSerialization();
            Assert.fail("Should have failed with some kind of invalid key message but got " + cs);
        }
        catch (JoseException e)
        {
            log.debug("Expected something like this: " + e);
        }
    }

    static void testBadKeyOnVerify(String compactSerialization, Key key) throws JoseException
    {
        JsonWebSignature jws = new JsonWebSignature();
        jws.setCompactSerialization(compactSerialization);
        jws.setKey(key);
        try
        {
            jws.verifySignature();
            Assert.fail("Should have failed with some kind of invalid key message");
        }
        catch (InvalidKeyException e)
        {
            log.debug("Expected something like this: " + e);
        }
    }
}

