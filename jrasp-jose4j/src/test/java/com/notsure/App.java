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

package com.notsure;

import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.jwk.*;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.keys.EllipticCurves;
import org.jose4j.keys.PbkdfKey;

import java.security.Provider;
import java.security.Security;
import java.util.LinkedList;
import java.util.List;

/**
 * Just a sandbox for messing with stuff
 */
public class App 
{
    public static void main(String... meh) throws Exception
    {
//String jwksJson =
//    "{\"keys\":[\n" +
//    " {\"kty\":\"EC\",\n\"kid\":\"4\",\n" +
//    "  \"x\":\"LX-7aQn7RAx3jDDTioNssbODUfED_6XvZP8NsGzMlRo\", \n" +
//    "  \"y\":\"dJbHEoeWzezPYuz6qjKJoRVLks7X8-BJXbewfyoJQ-A\",\n" +
//    "  \"crv\":\"P-256\"},\n" +
//    " {\"kty\":\"EC\",\n\"kid\":\"5\",\n" +
//    "  \"x\":\"f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU\",\n" +
//    "  \"y\":\"x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0\",\n" +
//    "  \"crv\":\"P-256\"},\n" +
//    " {\"kty\":\"EC\",\n\"kid\":\"6\",\n" +
//    "  \"x\":\"J8z237wci2YJAzArSdWIj4OgrOCCfuZ18WI77jsiS00\",\n" +
//    "  \"y\":\"5tTxvax8aRMMJ4unKdKsV0wcf3pOI3OG771gOa45wBU\",\n" +
//    "  \"crv\":\"P-256\"}\n" +
//    "]}";
//
//JsonWebKeySet jwks = new JsonWebKeySet(jwksJson);
//JsonWebKey jwk = jwks.findJsonWebKey("5", null, null, null);
//System.out.println(jwk.getKey());



//List<JsonWebKey> jwkList = new LinkedList<>();
//for (int kid = 4; kid < 7; kid++)
//{
//    JsonWebKey jwk = EcJwkGenerator.generateJwk(EllipticCurves.P256);
//    jwk.setKeyId(String.valueOf(kid));
//    jwkList.add(jwk);
//}
//JsonWebKeySet jwks = new JsonWebKeySet(jwkList);
//System.out.println(jwks.toJson(JsonWebKey.OutputControlLevel.PUBLIC_ONLY));

//JsonWebEncryption jwe = new JsonWebEncryption();
//jwe.setPayload("I actually really like Canada");
//jwe.setKey(new PbkdfKey("don't-tell-p@ul|pam!"));
//jwe.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.PBES2_HS256_A128KW);
//jwe.setEncryptionMethodHeaderParameter(ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);
//String compactSerialization = jwe.getCompactSerialization();
//
//System.out.println(compactSerialization);


//        String compactSerialization =
//            "eyJhbGciOiJQQkVTMi1IUzI1NitBMTI4S1ciLCJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwicDJjIjo4MTkyLCJwMnMiOiJRa2JMUW5pS0xVVFFWUDRsIn0." +
//            "g7s-MxHFn5WHCfO33hgWYiAtH1lB83TnufWoaFIEujEYb14pqeH9Mg." +
//            "6h172lww9VqemjMQMaVPdg." +
//            "YMg_F8aoT3ZByou3CURhKzaGX1nc5QJDo3cWyUSyow0." +
//            "Ie4iYLbdQCqwMWJf37rEZg";
//
//        JsonWebEncryption jwe = new JsonWebEncryption();
//        jwe.setCompactSerialization(compactSerialization);
//        jwe.setKey(new PbkdfKey("don't-tell-p@ul|pam!"));
//        String payload = jwe.getPayload();
//
//        System.out.println(payload);


//
//JsonWebKey jwk = JsonWebKey.Factory.newJwk("{\"kty\":\"EC\"," +
//    "\"kid\":\"my-first-key\"," +
//    "\"x\":\"xlKTWTx76fl9OZou4LHpDc3oHLC_vm-db7mdsFvO1JQ\"," +
//    "\"y\":\"3jXBG649Uqf7pf8RHO_jcJ8Jrhy23hjD933i6QEVNkk\"," +
//    "\"crv\":\"P-256\"}");
//
//String compactSerialization =
//    "eyJhbGciOiJFUzI1NiIsImtpZCI6Im15LWZpcnN0LWtleSJ9." +
//    "VVNBICMxIQ." +
//    "QJGB_sHj-w3yCBunJs2wxKgvZgG2Hq9PA-TDQEbNdTm2Wnj2sUSrBKZJAUREzF1FF25BbrgyohbKdGE1cB-hrA";
//
//
//JsonWebSignature jws = new JsonWebSignature();
//jws.setCompactSerialization(compactSerialization);
//jws.setKey(jwk.getKey());
//String payload = jws.getPayload();
//
//System.out.println(payload);


//
//PublicJsonWebKey jwk = EcJwkGenerator.generateJwk(EllipticCurves.P256);
//jwk.setKeyId("my-first-key");
//
//JsonWebSignature jws = new JsonWebSignature();
//jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256);
//jws.setPayload("USA #1!");
//jws.setKey(jwk.getPrivateKey());
//jws.setKeyIdHeaderValue(jwk.getKeyId());
//String compactSerialization = jws.getCompactSerialization();
//
//System.out.println(compactSerialization);
//
//
//        System.out.println(jws.getHeaders().getFullHeaderAsJsonString());
//        System.out.println(jwk.toJson(JsonWebKey.OutputControlLevel.INCLUDE_PRIVATE));
//        System.out.println(jwk.toJson(JsonWebKey.OutputControlLevel.PUBLIC_ONLY));
    }

    public static void dumpProviderInfo()
    {
        String version = System.getProperty("java.version");
        String vendor = System.getProperty("java.vendor");
        String home = System.getProperty("java.home");
        System.out.println("Java "+version+" from "+vendor+" at "+home+"");
        for (Provider provider : Security.getProviders())
        {
            System.out.println("Provider: " + provider.getName());
            for (Provider.Service service : provider.getServices())
            {
                System.out.println(" -> Algorithm: " + service.getAlgorithm());
            }
        }
    }

}
