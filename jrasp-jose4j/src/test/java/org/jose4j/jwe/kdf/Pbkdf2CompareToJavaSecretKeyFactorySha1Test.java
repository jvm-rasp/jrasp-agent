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
import org.jose4j.lang.StringUtil;
import org.junit.Assert;
import org.junit.Test;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.spec.KeySpec;

/**
 * At the time of writing java only seems to have a HMAC SHA1 version of PBKDF2 where JOSE
 * calls for SHA2. So I wrote my own PBKDF2 that can take an arbitrary HMAC. Seemed useful to
 * test my impl with SHA1 against the java impl a little bit.
 */
public class Pbkdf2CompareToJavaSecretKeyFactorySha1Test
{
    @Test
    public void testIterationCount() throws Exception
    {
        deriveAndCompare("somepass", "salty!", 1, 20);
        deriveAndCompare("somepass", "salty!", 2, 20);
        deriveAndCompare("somepass", "salty!", 3, 20);
        deriveAndCompare("somepass", "salty!", 4, 20);
        deriveAndCompare("somepass", "salty!", 100, 20);
    }

    @Test
    public void testIterationLength() throws Exception
    {
        deriveAndCompare("password", "sssss", 100, 4);
        deriveAndCompare("password", "sssss", 100, 16);
        deriveAndCompare("password", "sssss", 100, 20);
        deriveAndCompare("password", "sssss", 100, 21);
        deriveAndCompare("password", "sssss", 100, 32);
        deriveAndCompare("password", "sssss", 100, 64);
        deriveAndCompare("password", "sssss", 100, 65);
    }

    @Test
    public void testSomeRandoms() throws Exception
    {
        deriveAndCompare("pwd", "xxx", 1, 40);
        deriveAndCompare("alongerpasswordwithmorelettersinit", "abcdefghijklmnopqrstuv1234000001ccd", 10, 16);
        deriveAndCompare("password", "yyyy", 10, 1);
        deriveAndCompare("ppppppppp", "sssss", 1000, 21);
        deriveAndCompare("meh", "andmeh", 100, 20);
    }

    void deriveAndCompare(String p, String s, int c, int dkLen) throws Exception
    {
        PasswordBasedKeyDerivationFunction2 pbkdf2 = new PasswordBasedKeyDerivationFunction2("HmacSHA1");
        byte[] passwordBytes = StringUtil.getBytesAscii(p);
        byte[] saltBytes = StringUtil.getBytesAscii(s);
        byte[] derived = pbkdf2.derive(passwordBytes, saltBytes, c, dkLen);

        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec ks = new PBEKeySpec(p.toCharArray(), saltBytes, c, ByteUtil.bitLength(dkLen));
        SecretKey secretKey = f.generateSecret(ks);

        Assert.assertArrayEquals(secretKey.getEncoded(), derived);
    }
}
