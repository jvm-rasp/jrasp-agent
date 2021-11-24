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

import org.jose4j.base64url.Base64Url;
import org.jose4j.json.JsonUtil;
import org.jose4j.keys.EllipticCurves;
import org.jose4j.keys.ExampleEcKeysFromJws;
import org.jose4j.lang.JoseException;
import org.junit.Test;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.jose4j.jwk.JsonWebKey.OutputControlLevel.*;
import static org.junit.Assert.*;

/**
 */
public class EllipticCurveJsonWebKeyTest
{
	@Test
    public void testParseExampleWithPrivate256() throws JoseException
    {
        // key from http://tools.ietf.org/html/draft-ietf-jose-json-web-signature-13#appendix-A.3.1
        // it was shown as octets in -11 and before
        String jwkJson = "{\"kty\":\"EC\",\n" +
                   " \"crv\":\"P-256\",\n" +
                   " \"x\":\"f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU\",\n" +
                   " \"y\":\"x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0\",\n" +
                   " \"d\":\"jpsQnnGQmL-YBIffH1136cspYG6-0iY7X1fCE9-E9LI\"\n" +
                   "}";
        JsonWebKey jwk = JsonWebKey.Factory.newJwk(jwkJson);
        PublicJsonWebKey pubJwk = (PublicJsonWebKey) jwk;
        assertEquals(ExampleEcKeysFromJws.PRIVATE_256, pubJwk.getPrivateKey());
        assertEquals(ExampleEcKeysFromJws.PUBLIC_256, pubJwk.getPublicKey());
        assertEquals(EllipticCurves.P_256, ((EllipticCurveJsonWebKey)jwk).getCurveName());
    }

	@Test
    public void testFromKeyWithPrivate256() throws JoseException
    {
        PublicJsonWebKey jwk = PublicJsonWebKey.Factory.newPublicJwk(ExampleEcKeysFromJws.PUBLIC_256);
        assertEquals(EllipticCurves.P_256, ((EllipticCurveJsonWebKey)jwk).getCurveName());
        String jsonNoPrivateKey = jwk.toJson();
        jwk.setPrivateKey(ExampleEcKeysFromJws.PRIVATE_256);
        String d = "jpsQnnGQmL-YBIffH1136cspYG6-0iY7X1fCE9-E9LI";
        assertFalse(jwk.toJson().contains(d));
        assertEquals(jsonNoPrivateKey, jwk.toJson());

        assertFalse(jwk.toJson(PUBLIC_ONLY).contains(d));
        assertFalse(jwk.toJson().contains(d));
        assertFalse(jwk.toJson(INCLUDE_SYMMETRIC).contains(d));
        assertTrue(jwk.toJson(INCLUDE_PRIVATE).contains(d));
    }

	@Test
    public void testParseExampleWithPrivate512() throws JoseException
    {
        // this key also from http://tools.ietf.org/html/draft-ietf-jose-json-web-signature-13#appendix-A.3.1
        // it was shown as octets in -11 and before
        String jwkJson = "{\"kty\":\"EC\",\n" +
                " \"crv\":\"P-521\",\n" +
                " \"x\":\"AekpBQ8ST8a8VcfVOTNl353vSrDCLLJXmPk06wTjxrrjcBpXp5EOnYG_\n" +
                "      NjFZ6OvLFV1jSfS9tsz4qUxcWceqwQGk\",\n" +
                " \"y\":\"ADSmRA43Z1DSNx_RvcLI87cdL07l6jQyyBXMoxVg_l2Th-x3S1WDhjDl\n" +
                "      y79ajL4Kkd0AZMaZmh9ubmf63e3kyMj2\",\n" +
                " \"d\":\"AY5pb7A0UFiB3RELSD64fTLOSV_jazdF7fLYyuTw8lOfRhWg6Y6rUrPA\n" +
                "      xerEzgdRhajnu0ferB0d53vM9mE15j2C\"\n" +
                "}";
        JsonWebKey jwk = JsonWebKey.Factory.newJwk(jwkJson);
        PublicJsonWebKey pubJwk = (PublicJsonWebKey) jwk;
        assertEquals(ExampleEcKeysFromJws.PRIVATE_521, pubJwk.getPrivateKey());
        assertEquals(ExampleEcKeysFromJws.PUBLIC_521, pubJwk.getPublicKey());
        assertEquals(EllipticCurves.P_521, ((EllipticCurveJsonWebKey)jwk).getCurveName());
    }

	@Test
    public void testFromKeyWithPrivate512() throws JoseException
    {
        PublicJsonWebKey jwk = PublicJsonWebKey.Factory.newPublicJwk(ExampleEcKeysFromJws.PUBLIC_521);
        assertEquals(EllipticCurves.P_521, ((EllipticCurveJsonWebKey)jwk).getCurveName());
        String jsonNoPrivateKey = jwk.toJson();
        jwk.setPrivateKey(ExampleEcKeysFromJws.PRIVATE_521);
        String d = "AY5pb7A0UFiB3RELSD64fTLOSV_jazdF7fLYyuTw8lOfRhWg6Y6rUrPAxerEzgdRhajnu0ferB0d53vM9mE15j2C";
        assertFalse(jwk.toJson().contains(d));
        assertEquals(jsonNoPrivateKey, jwk.toJson());

        assertFalse(jwk.toJson(PUBLIC_ONLY).contains(d));
        assertFalse(jwk.toJson().contains(d));
        assertFalse(jwk.toJson(INCLUDE_SYMMETRIC).contains(d));
        assertTrue(jwk.toJson(INCLUDE_PRIVATE).contains(d));

        System.out.println(jwk);
    }
	
	@Test
	public void testToJsonWithPublicKeyOnlyJWKAndIncludePrivateSettings() throws JoseException
    {
        PublicJsonWebKey jwk = PublicJsonWebKey.Factory.newPublicJwk(ExampleEcKeysFromJws.PUBLIC_521);
        String jsonNoPrivateKey = jwk.toJson(PUBLIC_ONLY);
        PublicJsonWebKey publicOnlyJWK = PublicJsonWebKey.Factory.newPublicJwk(jsonNoPrivateKey);
        assertThat(jsonNoPrivateKey,is(equalTo(publicOnlyJWK.toJson(INCLUDE_PRIVATE))));
	}

    @Test
    public void testCryptoBinaryThread() throws Exception
    {
        // make sure that "The length of [the y] octet string MUST
        //   be the full size of a coordinate for the curve specified in the "crv"
        //  parameter."
        String keySpec = "MIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQBCCAc9n4N7ZOr_tTu" +
                "_wAOmPKi4qTp5X3su6O3010hxmBYj9zI4u_0dm6UZa0LsjdfvcAET6vH3mEApvGKpDWrRsAA_nJhyQ20ca7Nn0Zvyiq54FfCAblGK7kuduF" +
                "BTPkxv9eOjiaeGp7V_f3qV1kxS_Il2LY7Tc5l2GSlW_-SzYKxgek";

        Base64Url base64Url = new Base64Url();
        byte[] bytes = base64Url.base64UrlDecode(keySpec);
        PublicKey ecPubKey = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(bytes));

        PublicJsonWebKey jwk = EllipticCurveJsonWebKey.Factory.newPublicJwk(ecPubKey);
        String jwkJson = jwk.toJson(PUBLIC_ONLY);
        Map<String,Object> parsed = JsonUtil.parseJson(jwkJson);
        String x = (String)parsed.get(EllipticCurveJsonWebKey.X_MEMBER_NAME);
        assertThat("AQggHPZ-De2Tq_7U7v8ADpjyouKk6eV97Lujt9NdIcZgWI_cyOLv9HZulGWtC7I3X73ABE-rx95hAKbxiqQ1q0bA", is(equalTo(x)));
        String y = (String)parsed.get(EllipticCurveJsonWebKey.Y_MEMBER_NAME);
        assertThat("AP5yYckNtHGuzZ9Gb8oqueBXwgG5Riu5LnbhQUz5Mb_Xjo4mnhqe1f396ldZMUvyJdi2O03OZdhkpVv_ks2CsYHp", is(equalTo(y)));

        // we will be liberal and accept either
        String noLeftZeroPaddingBytes = "{\"kty\":\"EC\",\"x\":\"AQggHPZ-De2Tq_7U7v8ADpjyouKk6eV97Lujt9NdIcZgWI_cyOLv9HZulGWtC7I3X73ABE-rx95hAKbxiqQ1q0bA\",\"y\":\"_nJhyQ20ca7Nn0Zvyiq54FfCAblGK7kuduFBTPkxv9eOjiaeGp7V_f3qV1kxS_Il2LY7Tc5l2GSlW_-SzYKxgek\",\"crv\":\"P-521\"}";
        String withLeftZeroPaddingBytes = "{\"kty\":\"EC\",\"x\":\"AQggHPZ-De2Tq_7U7v8ADpjyouKk6eV97Lujt9NdIcZgWI_cyOLv9HZulGWtC7I3X73ABE-rx95hAKbxiqQ1q0bA\",\"y\":\"AP5yYckNtHGuzZ9Gb8oqueBXwgG5Riu5LnbhQUz5Mb_Xjo4mnhqe1f396ldZMUvyJdi2O03OZdhkpVv_ks2CsYHp\",\"crv\":\"P-521\"}";
        PublicJsonWebKey jwkWithNoZeroLeftPaddingBytes = EllipticCurveJsonWebKey.Factory.newPublicJwk(noLeftZeroPaddingBytes);
        PublicJsonWebKey jwkWithZeroLeftPaddingBytes = EllipticCurveJsonWebKey.Factory.newPublicJwk(withLeftZeroPaddingBytes);

        assertEquals(jwkWithNoZeroLeftPaddingBytes.getPublicKey(), jwkWithZeroLeftPaddingBytes.getPublicKey());

    }
}
