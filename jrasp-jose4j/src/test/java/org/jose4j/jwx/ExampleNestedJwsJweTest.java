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

import junit.framework.TestCase;
import org.jose4j.json.JsonUtil;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.IntDate;
import org.jose4j.jwt.ReservedClaimNames;
import org.jose4j.keys.ExampleRsaJwksFromJwe;
import org.jose4j.keys.ExampleRsaKeyFromJws;
import org.jose4j.lang.JoseException;
import org.jose4j.lang.JsonHelp;

import java.util.Map;

/**
 */
public class ExampleNestedJwsJweTest extends TestCase
{
    public void testExample() throws JoseException
    {
        // Test Example Nested JWT from http://tools.ietf.org/html/draft-ietf-oauth-json-web-token-11#appendix-A.2
        String jwtJson = "eyJhbGciOiJSU0ExXzUiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiY3R5IjoiSldU" +
                "In0." +
                "g_hEwksO1Ax8Qn7HoN-BVeBoa8FXe0kpyk_XdcSmxvcM5_P296JXXtoHISr_DD_M" +
                "qewaQSH4dZOQHoUgKLeFly-9RI11TG-_Ge1bZFazBPwKC5lJ6OLANLMd0QSL4fYE" +
                "b9ERe-epKYE3xb2jfY1AltHqBO-PM6j23Guj2yDKnFv6WO72tteVzm_2n17SBFvh" +
                "DuR9a2nHTE67pe0XGBUS_TK7ecA-iVq5COeVdJR4U4VZGGlxRGPLRHvolVLEHx6D" +
                "YyLpw30Ay9R6d68YCLi9FYTq3hIXPK_-dmPlOUlKvPr1GgJzRoeC9G5qCvdcHWsq" +
                "JGTO_z3Wfo5zsqwkxruxwA." +
                "UmVkbW9uZCBXQSA5ODA1Mg." +
                "VwHERHPvCNcHHpTjkoigx3_ExK0Qc71RMEParpatm0X_qpg-w8kozSjfNIPPXiTB" +
                "BLXR65CIPkFqz4l1Ae9w_uowKiwyi9acgVztAi-pSL8GQSXnaamh9kX1mdh3M_TT" +
                "-FZGQFQsFhu0Z72gJKGdfGE-OE7hS1zuBD5oEUfk0Dmb0VzWEzpxxiSSBbBAzP10" +
                "l56pPfAtrjEYw-7ygeMkwBl6Z_mLS6w6xUgKlvW6ULmkV-uLC4FUiyKECK4e3WZY" +
                "Kw1bpgIqGYsw2v_grHjszJZ-_I5uM-9RA8ycX9KqPRp9gc6pXmoU_-27ATs9XCvr" +
                "ZXUtK2902AUzqpeEUJYjWWxSNsS-r1TJ1I-FMJ4XyAiGrfmo9hQPcNBYxPz3GQb2" +
                "8Y5CLSQfNgKSGt0A4isp1hBUXBHAndgtcslt7ZoQJaKe_nNJgNliWtWpJ_ebuOpE" +
                "l8jdhehdccnRMIwAmU1n7SPkmhIl1HlSOpvcvDfhUN5wuqU955vOBvfkBOh5A11U" +
                "zBuo2WlgZ6hYi9-e3w29bR0C2-pp3jbqxEDw3iWaf2dc5b-LnR0FEYXvI_tYk5rd" +
                "_J9N0mg0tQ6RbpxNEMNoA9QWk5lgdPvbh9BaO195abQ." +
                "AVO9iT5AV4CzvDJCdhSFlQ";

        JsonWebEncryption jwe = new JsonWebEncryption();
        jwe.setCompactSerialization(jwtJson);
        jwe.setKey(ExampleRsaJwksFromJwe.APPENDIX_A_2.getPrivateKey());
        String jwsJson = jwe.getPlaintextString();
        System.out.println(jwsJson);
        JsonWebSignature jws = new JsonWebSignature();
        jws.setCompactSerialization(jwsJson);
        jws.setKey(ExampleRsaKeyFromJws.PUBLIC_KEY);
        System.out.println(jws.verifySignature());
        System.out.println(jws.getPayload());
        Map<String, Object> claims = JsonUtil.parseJson(jws.getPayload());
        assertEquals("joe", claims.get(ReservedClaimNames.ISSUER));
        assertTrue((Boolean) claims.get("http://example.com/is_root"));
        assertEquals(IntDate.fromSeconds(1300819380), JsonHelp.getIntDate(claims, ReservedClaimNames.EXPIRATION_TIME));
    }

}
