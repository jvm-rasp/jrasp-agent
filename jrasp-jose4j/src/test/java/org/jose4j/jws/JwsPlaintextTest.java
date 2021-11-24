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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.keys.HmacKey;
import org.jose4j.lang.InvalidAlgorithmException;
import org.jose4j.lang.InvalidKeyException;
import org.jose4j.lang.JoseException;
import org.junit.Test;

import java.security.Key;

import static org.hamcrest.CoreMatchers.is;
import static org.jose4j.jwa.AlgorithmConstraints.ConstraintType.BLACKLIST;
import static org.jose4j.jwa.AlgorithmConstraints.ConstraintType.WHITELIST;
import static org.jose4j.jws.AlgorithmIdentifiers.NONE;
import static org.junit.Assert.*;

/**
 */
public class JwsPlaintextTest
{
    Log log = LogFactory.getLog(this.getClass());

    String JWS = "eyJhbGciOiJub25lIn0.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.";
    String PAYLOAD = "{\"iss\":\"joe\",\r\n \"exp\":1300819380,\r\n \"http://example.com/is_root\":true}";
    Key KEY = new HmacKey(new byte[] {1,2,3,4,5,-3,28,123,-53});

    @Test
    public void testExampleDecode() throws JoseException
    {
        JsonWebSignature jws = new JsonWebSignature();
        jws.setAlgorithmConstraints(AlgorithmConstraints.ALLOW_ONLY_NONE);
        jws.setCompactSerialization(JWS);
        assertTrue(jws.verifySignature());
        String payload = jws.getPayload();
        assertEquals(PAYLOAD, payload);
    }

    @Test
    public void testExampleEncode() throws JoseException
    {
        JsonWebSignature jws = new JsonWebSignature();
        jws.setAlgorithmConstraints(AlgorithmConstraints.ALLOW_ONLY_NONE);
        jws.setAlgorithmHeaderValue(NONE);
        jws.setPayload(PAYLOAD);
        assertEquals(JWS, jws.getCompactSerialization());
    }

    @Test(expected = InvalidKeyException.class)
    public void testSignWithKeyNoGood() throws JoseException
    {
        JsonWebSignature jws = new JsonWebSignature();
        jws.setAlgorithmConstraints(AlgorithmConstraints.NO_CONSTRAINTS);
        jws.setAlgorithmHeaderValue(NONE);
        jws.setPayload(PAYLOAD);
        jws.setKey(KEY);
        jws.getCompactSerialization();
    }

    @Test(expected = InvalidKeyException.class)
    public void testExampleVerifyWithKeyNoGood() throws JoseException
    {
        JsonWebSignature jws = new JsonWebSignature();
        jws.setAlgorithmConstraints(AlgorithmConstraints.NO_CONSTRAINTS);
        jws.setCompactSerialization(JWS);
        jws.setKey(KEY);
        jws.verifySignature();
    }

    @Test(expected = InvalidAlgorithmException.class)
    public void testExampleVerifyButNoneNotAllowed() throws JoseException
    {
        JsonWebSignature jws = new JsonWebSignature();
        jws.setAlgorithmConstraints(new AlgorithmConstraints(BLACKLIST, NONE));
        jws.setCompactSerialization(JWS);
        jws.verifySignature();
    }

    @Test
    public void testExampleVerifyWithOnlyNoneAllowed() throws JoseException
    {
        JsonWebSignature jws = new JsonWebSignature();
        jws.setAlgorithmConstraints(new AlgorithmConstraints(WHITELIST, NONE));
        jws.setCompactSerialization(JWS);
        assertThat(jws.verifySignature(), is(true));
    }

    @Test
    public void testADecode() throws JoseException
    {
        String cs = "eyJhbGciOiJub25lIn0.eyJhdXRoX3RpbWUiOjEzMzk2MTMyNDgsImV4cCI6MTMzOTYxMzU0OCwiaXNzIjoiaHR0cHM6XC9cL2V4YW1wbGUuY29tIiwiYXVkIjoiYSIsImp0aSI6ImpJQThxYTM1QXJvVjZpUDJxNHdSQWwiLCJ1c2VyX2lkIjoiam9obiIsImlhdCI6MTMzOTYxMzI0OCwiYWNyIjozfQ.";
        JsonWebSignature jws = new JsonWebSignature();
        jws.setAlgorithmConstraints(AlgorithmConstraints.NO_CONSTRAINTS);
        jws.setCompactSerialization(cs);
        assertTrue(jws.verifySignature());
        String payload = jws.getPayload();
        log.debug(payload);
    }
}
