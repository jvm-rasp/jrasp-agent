/*
 * Copyright 2012-2014 Brian Campbell
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
package org.jose4j.jwe.kdf;

import org.jose4j.lang.ByteUtil;
import org.jose4j.lang.InvalidKeyException;
import org.jose4j.lang.StringUtil;
import org.jose4j.mac.MacUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 */
public class Pbkdf2JwkExampleTest
{
    @Test
    public void testThePbdkfPartFromJwkAppendixC() throws IOException, InvalidKeyException
    {
        // just the pbkdf2 part from http://tools.ietf.org/html/draft-ietf-jose-json-web-key-22#appendix-C

        String pass = "Thus from my lips, by yours, my sin is purged.";

        // The Salt value (UTF8(Alg) || 0x00 || Salt Input) is:
        byte[] saltValue = ByteUtil.convertUnsignedToSignedTwosComp(new int[]{80, 66, 69, 83, 50, 45, 72, 83, 50, 53, 54, 43, 65, 49, 50, 56, 75,
                87, 0, 217, 96, 147, 112, 150, 117, 70, 247, 127, 8, 155, 137, 174,
                42, 80, 215});

        int iterationCount = 4096;

        PasswordBasedKeyDerivationFunction2 pbkdf2 = new PasswordBasedKeyDerivationFunction2(MacUtil.HMAC_SHA256);
        byte[] derived = pbkdf2.derive(StringUtil.getBytesUtf8(pass), saltValue, iterationCount, 16);
        byte[] expectedDerived = ByteUtil.convertUnsignedToSignedTwosComp(new int[]{110, 171, 169, 92, 129, 92, 109, 117, 233, 242, 116, 233, 170, 14, 24, 75});
        Assert.assertArrayEquals(expectedDerived, derived);
    }
}
