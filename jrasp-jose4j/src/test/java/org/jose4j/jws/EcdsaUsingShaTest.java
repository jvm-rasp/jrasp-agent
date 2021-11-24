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
import org.jose4j.keys.*;
import org.jose4j.lang.JoseException;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 */
public class EcdsaUsingShaTest extends TestCase
{
    EcKeyUtil keyUtil = new EcKeyUtil();

    public void testP256RoundTripGenKeys() throws JoseException
    {
        KeyPair keyPair1 = keyUtil.generateKeyPair(EllipticCurves.P256);
        KeyPair keyPair2 = keyUtil.generateKeyPair(EllipticCurves.P256);
        String algo = AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256;
        PrivateKey priv1 = keyPair1.getPrivate();
        PublicKey pub1 = keyPair1.getPublic();
        PrivateKey priv2 = keyPair2.getPrivate();
        PublicKey pub2 = keyPair2.getPublic();
        JwsTestSupport.testBasicRoundTrip("PAYLOAD!!!", algo, priv1, pub1, priv2, pub2);
    }

    public void testP384RoundTripGenKeys() throws JoseException
    {
        KeyPair keyPair1 = keyUtil.generateKeyPair(EllipticCurves.P384);
        KeyPair keyPair2 = keyUtil.generateKeyPair(EllipticCurves.P384);
        String algo = AlgorithmIdentifiers.ECDSA_USING_P384_CURVE_AND_SHA384;
        PrivateKey priv1 = keyPair1.getPrivate();
        PublicKey pub1 = keyPair1.getPublic();
        PrivateKey priv2 = keyPair2.getPrivate();
        PublicKey pub2 = keyPair2.getPublic();
        JwsTestSupport.testBasicRoundTrip("The umlaut ( /??mla?t/ uum-lowt) refers to a sound shift.", algo, priv1, pub1, priv2, pub2);
    }

    public void testP521RoundTripGenKeys() throws JoseException
    {
        KeyPair keyPair1 = keyUtil.generateKeyPair(EllipticCurves.P521);
        KeyPair keyPair2 = keyUtil.generateKeyPair(EllipticCurves.P521);
        String algo = AlgorithmIdentifiers.ECDSA_USING_P521_CURVE_AND_SHA512;
        PrivateKey priv1 = keyPair1.getPrivate();
        PublicKey pub1 = keyPair1.getPublic();
        PrivateKey priv2 = keyPair2.getPrivate();
        PublicKey pub2 = keyPair2.getPublic();
        JwsTestSupport.testBasicRoundTrip("?????", algo, priv1, pub1, priv2, pub2);
    }

    public void testP256RoundTripExampleKeysAndGenKeys() throws JoseException
    {
        String algo = AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256;
        PrivateKey priv1 = ExampleEcKeysFromJws.PRIVATE_256;
        PublicKey pub1 = ExampleEcKeysFromJws.PUBLIC_256;
        KeyPair keyPair = keyUtil.generateKeyPair(EllipticCurves.P256);
        PrivateKey priv2 = keyPair.getPrivate();
        PublicKey pub2 = keyPair.getPublic();
        JwsTestSupport.testBasicRoundTrip("something here", algo, priv1, pub1, priv2, pub2);
    }

    public void testP521RoundTripExampleKeysAndGenKeys() throws JoseException
    {
        String algo = AlgorithmIdentifiers.ECDSA_USING_P521_CURVE_AND_SHA512;
        PrivateKey priv1 = ExampleEcKeysFromJws.PRIVATE_521;
        PublicKey pub1 = ExampleEcKeysFromJws.PUBLIC_521;
        KeyPair keyPair = keyUtil.generateKeyPair(EllipticCurves.P521);
        PrivateKey priv2 = keyPair.getPrivate();
        PublicKey pub2 = keyPair.getPublic();
        JwsTestSupport.testBasicRoundTrip("touch�", algo, priv1, pub1, priv2, pub2);
    }

    public void testBadKeys() throws JoseException
    {
        String cs256 = "eyJhbGciOiJFUzI1NiJ9.UEFZTE9BRCEhIQ.WcL6cqkJSkzwK4Y85Lj96l-_WVmII6foW8d7CJNgdgDxi6NnTdXQD1Ze2vdXGcErIu9sJX9EXkmiaHSd0GQkgA";
        String cs384 = "eyJhbGciOiJFUzM4NCJ9.VGhlIHVtbGF1dCAoIC8_P21sYT90LyB1dW0tbG93dCkgcmVmZXJzIHRvIGEgc291bmQgc2hpZnQu.UO2zG037CLktsDeHJ71w48DmTMmCjsEEKhFGSE1uBQUG8rRZousdJR8p2rykZglU2RdWG48AE4Rf5_WfiZuP5ANC_bLgiOz1rwlSe6ds2romfdQ-enn7KTvr9Cmqt2Ot";
        String cs512 = "eyJhbGciOiJFUzUxMiJ9.Pz8_Pz8.AJS7SrxiK6zpJkXjV4iWM_oUcE294hV3RK-y5uQD2Otx-UwZNFEH6L66ww5ukQ7R1rykiWd9PNjzlzrgwfJqF2KyASmO6Hz7dZr9EYPIX6rrEpWjsp1tDJ0_Hq45Rk2eJ5z3cFTIpVu6V7CGXwVWvVCDQzcGpmZIFR939aI49Z_HWT7b";
        for (String cs : new String[] {cs256, cs384, cs512})
        {
            JwsTestSupport.testBadKeyOnVerify(cs, ExampleRsaKeyFromJws.PRIVATE_KEY);
            JwsTestSupport.testBadKeyOnVerify(cs, null);
            JwsTestSupport.testBadKeyOnVerify(cs, new HmacKey(new byte[2048]));
            JwsTestSupport.testBadKeyOnVerify(cs, ExampleRsaKeyFromJws.PUBLIC_KEY);
            JwsTestSupport.testBadKeyOnVerify(cs, ExampleEcKeysFromJws.PRIVATE_256);
            JwsTestSupport.testBadKeyOnVerify(cs, ExampleEcKeysFromJws.PRIVATE_521);
        }

        JwsTestSupport.testBadKeyOnVerify(cs256, ExampleEcKeysFromJws.PUBLIC_521);
        JwsTestSupport.testBadKeyOnVerify(cs384, ExampleEcKeysFromJws.PUBLIC_521);
        JwsTestSupport.testBadKeyOnVerify(cs384, ExampleEcKeysFromJws.PUBLIC_256);
        JwsTestSupport.testBadKeyOnVerify(cs512, ExampleEcKeysFromJws.PUBLIC_256);
    }
}
