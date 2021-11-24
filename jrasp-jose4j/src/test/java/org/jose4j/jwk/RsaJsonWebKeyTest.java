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


import org.jose4j.keys.ExampleRsaKeyFromJws;
import org.jose4j.lang.JoseException;
import org.junit.Test;

import java.security.interfaces.RSAPrivateCrtKey;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.jose4j.jwk.JsonWebKey.OutputControlLevel.*;
import static org.junit.Assert.*;

/**
 */
public class RsaJsonWebKeyTest
{
    // key from http://tools.ietf.org/html/draft-ietf-jose-json-web-signature-13#appendix-A.3.1
    // it was shown as octets in -11 and before
    private static final String RSA_JWK_WITH_PRIVATE_KEY =
            "{\"kty\":\"RSA\",\n" +
            " \"n\":\"ofgWCuLjybRlzo0tZWJjNiuSfb4p4fAkd_wWJcyQoTbji9k0l8W26mPddx\n" +
            "      HmfHQp-Vaw-4qPCJrcS2mJPMEzP1Pt0Bm4d4QlL-yRT-SFd2lZS-pCgNMs\n" +
            "      D1W_YpRPEwOWvG6b32690r2jZ47soMZo9wGzjb_7OMg0LOL-bSf63kpaSH\n" +
            "      SXndS5z5rexMdbBYUsLA9e-KXBdQOS-UTo7WTBEMa2R2CapHg665xsmtdV\n" +
            "      MTBQY4uDZlxvb3qCo5ZwKh9kG4LT6_I5IhlJH7aGhyxXFvUK-DWNmoudF8\n" +
            "      NAco9_h9iaGNj8q2ethFkMLs91kzk2PAcDTW9gb54h4FRWyuXpoQ\",\n" +
            " \"e\":\"AQAB\",\n" +
            " \"d\":\"Eq5xpGnNCivDflJsRQBXHx1hdR1k6Ulwe2JZD50LpXyWPEAeP88vLNO97I\n" +
            "      jlA7_GQ5sLKMgvfTeXZx9SE-7YwVol2NXOoAJe46sui395IW_GO-pWJ1O0\n" +
            "      BkTGoVEn2bKVRUCgu-GjBVaYLU6f3l9kJfFNS3E0QbVdxzubSu3Mkqzjkn\n" +
            "      439X0M_V51gfpRLI9JYanrC4D4qAdGcopV_0ZHHzQlBjudU2QvXt4ehNYT\n" +
            "      CBr6XCLQUShb1juUO1ZdiYoFaFQT5Tw8bGUl_x_jTj3ccPDVZFD9pIuhLh\n" +
            "      BOneufuBiB4cS98l2SR_RQyGWSeWjnczT0QU91p1DhOVRuOopznQ\"\n" +
            "}";

    @Test
    public void testParseExampleWithPrivate() throws JoseException
    {
        JsonWebKey jwk = JsonWebKey.Factory.newJwk(RSA_JWK_WITH_PRIVATE_KEY);
        PublicJsonWebKey pubJwk = (PublicJsonWebKey) jwk;
        assertEquals(ExampleRsaKeyFromJws.PRIVATE_KEY, pubJwk.getPrivateKey());
        assertEquals(ExampleRsaKeyFromJws.PUBLIC_KEY, pubJwk.getPublicKey());
    }

    @Test
    public void testFromKeyWithPrivate() throws JoseException
    {
        PublicJsonWebKey jwk = PublicJsonWebKey.Factory.newPublicJwk(ExampleRsaKeyFromJws.PUBLIC_KEY);
        String jsonNoPrivateKey = jwk.toJson();
        jwk.setPrivateKey(ExampleRsaKeyFromJws.PRIVATE_KEY);
        String dKey = "\"" + RsaJsonWebKey.PRIVATE_EXPONENT_MEMBER_NAME + "\"";
        assertFalse(jwk.toJson().contains(dKey));
        assertEquals(jsonNoPrivateKey, jwk.toJson());

        assertTrue(jwk.toJson(INCLUDE_PRIVATE).contains(dKey));
    }

    @Test
    public void testFromKeyWithCrtPrivateAndBackAndAgain() throws JoseException
    {
        String json = "{\"kty\":\"RSA\",\n" +
                "          \"n\":\"0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4\n" +
                "     cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMst\n" +
                "     n64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2Q\n" +
                "     vzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbIS\n" +
                "     D08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniIqbw\n" +
                "     0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw\",\n" +
                "          \"e\":\"AQAB\",\n" +
                "          \"d\":\"X4cTteJY_gn4FYPsXB8rdXix5vwsg1FLN5E3EaG6RJoVH-HLLKD9\n" +
                "     M7dx5oo7GURknchnrRweUkC7hT5fJLM0WbFAKNLWY2vv7B6NqXSzUvxT0_YSfqij\n" +
                "     wp3RTzlBaCxWp4doFk5N2o8Gy_nHNKroADIkJ46pRUohsXywbReAdYaMwFs9tv8d\n" +
                "     _cPVY3i07a3t8MN6TNwm0dSawm9v47UiCl3Sk5ZiG7xojPLu4sbg1U2jx4IBTNBz\n" +
                "     nbJSzFHK66jT8bgkuqsk0GjskDJk19Z4qwjwbsnn4j2WBii3RL-Us2lGVkY8fkFz\n" +
                "     me1z0HbIkfz0Y6mqnOYtqc0X4jfcKoAC8Q\",\n" +
                "          \"p\":\"83i-7IvMGXoMXCskv73TKr8637FiO7Z27zv8oj6pbWUQyLPQBQxtPV\n" +
                "     nwD20R-60eTDmD2ujnMt5PoqMrm8RfmNhVWDtjjMmCMjOpSXicFHj7XOuVIYQyqV\n" +
                "     WlWEh6dN36GVZYk93N8Bc9vY41xy8B9RzzOGVQzXvNEvn7O0nVbfs\",\n" +
                "          \"q\":\"3dfOR9cuYq-0S-mkFLzgItgMEfFzB2q3hWehMuG0oCuqnb3vobLyum\n" +
                "     qjVZQO1dIrdwgTnCdpYzBcOfW5r370AFXjiWft_NGEiovonizhKpo9VVS78TzFgx\n" +
                "     kIdrecRezsZ-1kYd_s1qDbxtkDEgfAITAG9LUnADun4vIcb6yelxk\",\n" +
                "          \"dp\":\"G4sPXkc6Ya9y8oJW9_ILj4xuppu0lzi_H7VTkS8xj5SdX3coE0oim\n" +
                "     YwxIi2emTAue0UOa5dpgFGyBJ4c8tQ2VF402XRugKDTP8akYhFo5tAA77Qe_Nmtu\n" +
                "     YZc3C3m3I24G2GvR5sSDxUyAN2zq8Lfn9EUms6rY3Ob8YeiKkTiBj0\",\n" +
                "          \"dq\":\"s9lAH9fggBsoFR8Oac2R_E2gw282rT2kGOAhvIllETE1efrA6huUU\n" +
                "     vMfBcMpn8lqeW6vzznYY5SSQF7pMdC_agI3nG8Ibp1BUb0JUiraRNqUfLhcQb_d9\n" +
                "     GF4Dh7e74WbRsobRonujTYN1xCaP6TO61jvWrX-L18txXw494Q_cgk\",\n" +
                "          \"qi\":\"GyM_p6JrXySiz1toFgKbWV-JdI3jQ4ypu9rbMWx3rQJBfmt0FoYzg\n" +
                "     UIZEVFEcOqwemRN81zoDAaa-Bk0KWNGDjJHZDdDmFhW3AN7lI-puxk_mHZGJ11rx\n" +
                "     yR8O55XLSe3SPmRfKwZI6yU24ZxvQKFYItdldUKGzO6Ia6zTKhAVRU\",\n" +
                "          \"alg\":\"RS256\",\n" +
                "          \"kid\":\"2011-04-29\"}";
        PublicJsonWebKey jwk = PublicJsonWebKey.Factory.newPublicJwk(json);

        assertTrue(jwk.getPrivateKey() instanceof RSAPrivateCrtKey);

        String jsonOut = jwk.toJson(PUBLIC_ONLY);
        assertFalse(jsonOut.contains("\"d\""));
        assertFalse(jsonOut.contains("\"p\""));
        assertFalse(jsonOut.contains("\"q\""));
        assertFalse(jsonOut.contains("\"dp\""));
        assertFalse(jsonOut.contains("\"dq\""));
        assertFalse(jsonOut.contains("\"qi\""));

        jsonOut = jwk.toJson();
        assertFalse(jsonOut.contains("\"d\""));
        assertFalse(jsonOut.contains("\"p\""));
        assertFalse(jsonOut.contains("\"q\""));
        assertFalse(jsonOut.contains("\"dp\""));
        assertFalse(jsonOut.contains("\"dq\""));
        assertFalse(jsonOut.contains("\"qi\""));

        jsonOut = jwk.toJson(INCLUDE_SYMMETRIC);
        assertFalse(jsonOut.contains("\"d\""));
        assertFalse(jsonOut.contains("\"p\""));
        assertFalse(jsonOut.contains("\"q\""));
        assertFalse(jsonOut.contains("\"dp\""));
        assertFalse(jsonOut.contains("\"dq\""));
        assertFalse(jsonOut.contains("\"qi\""));

        jsonOut = jwk.toJson(INCLUDE_PRIVATE);
        assertTrue(jsonOut.contains("\"d\""));
        assertTrue(jsonOut.contains("\"p\""));
        assertTrue(jsonOut.contains("\"q\""));
        assertTrue(jsonOut.contains("\"dp\""));
        assertTrue(jsonOut.contains("\"dq\""));
        assertTrue(jsonOut.contains("\"qi\""));

        PublicJsonWebKey jwkAgain = PublicJsonWebKey.Factory.newPublicJwk(jsonOut);

        assertTrue(jwkAgain.getPrivateKey() instanceof RSAPrivateCrtKey);
        assertEquals(jwk.getPrivateKey(), jwkAgain.getPrivateKey());
    }
    
	@Test
	public void testToJsonWithPublicKeyOnlyJWKAndIncludePrivateSettings() throws JoseException
    {
        PublicJsonWebKey jwk = PublicJsonWebKey.Factory.newPublicJwk(ExampleRsaKeyFromJws.PUBLIC_KEY);
        String jsonNoPrivateKey = jwk.toJson(PUBLIC_ONLY);
        PublicJsonWebKey publicOnlyJWK = PublicJsonWebKey.Factory.newPublicJwk(jsonNoPrivateKey);
        assertThat(jsonNoPrivateKey,is(equalTo(publicOnlyJWK.toJson(INCLUDE_PRIVATE))));
	}

}
