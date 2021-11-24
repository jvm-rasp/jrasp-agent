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

package org.jose4j.jwe.kdf;

import junit.framework.TestCase;
import org.jose4j.base64url.Base64Url;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.lang.ByteUtil;
import org.jose4j.lang.StringUtil;

import java.util.Arrays;

/**
 */
public class ConcatKeyDerivationFunctionTest extends TestCase
{
    public void testGetReps()
    {
        ConcatKeyDerivationFunction kdf = new ConcatKeyDerivationFunction("SHA-256");
        assertEquals(1, kdf.getReps(256));
        assertEquals(2, kdf.getReps(384));
        assertEquals(2, kdf.getReps(512));
        assertEquals(4, kdf.getReps(1024));
        assertEquals(5, kdf.getReps(1032));
        assertEquals(8, kdf.getReps(2048));
        assertEquals(9, kdf.getReps(2056));
    }

    public void testGetDatalenData()
    {
        String apu = "QWxpY2U";
        KdfUtil kdfUtil = new KdfUtil();
        byte[] apuDatalenData = kdfUtil.getDatalenDataFormat(apu);
        assertTrue(Arrays.equals(apuDatalenData, new byte[] {0, 0, 0, 5, 65, 108, 105, 99, 101}));

        String apv = "Qm9i";
        byte[] apvDatalenData = kdfUtil.getDatalenDataFormat(apv);
        assertTrue(Arrays.equals(apvDatalenData, new byte[] {0, 0, 0, 3, 'B', 'o', 'b'}));

        assertTrue(Arrays.equals(kdfUtil.prependDatalen(new byte[]{}), new byte[] {0, 0, 0, 0}));
        assertTrue(Arrays.equals(kdfUtil.prependDatalen(null), new byte[] {0, 0, 0, 0}));
    }

    public void testKdf1() throws Exception
    {
        // test values produced from implementation found at http://stackoverflow.com/questions/10879658
        String derivedKey = "pgs50IOZ6BxfqvTSie4t9OjWxGr4whiHo1v9Dti93CRiJE2PP60FojLatVVrcjg3BxpuFjnlQxL97GOwAfcwLA";
        byte[] z = Base64Url.decode("Sq8rGLm4rEtzScmnSsY5r1n-AqBl_iBU8FxN80Uc0S0");
        System.out.println(Base64Url.encode(z));
        KdfUtil kdfUtil = new KdfUtil();
        int keyDatalen = 512;
        String alg = ContentEncryptionAlgorithmIdentifiers.AES_256_CBC_HMAC_SHA_512;
        byte[] algId = kdfUtil.prependDatalen(StringUtil.getBytesUtf8(alg));
        byte[] partyU = new byte[] {0, 0, 0, 0};
        byte[] partyV = new byte[] {0, 0, 0, 0};
        byte[] pub = ByteUtil.getBytes(keyDatalen);
        byte[] priv = ByteUtil.EMPTY_BYTES;

        ConcatKeyDerivationFunction myConcatKdf = new ConcatKeyDerivationFunction("SHA-256");

        byte[] kdfed = myConcatKdf.kdf(z, keyDatalen, algId, partyU, partyV, pub, priv);
        assertEquals(derivedKey, Base64Url.encode(kdfed));

    }

    public void testKdf2() throws Exception
    {
        // test values produced from implementation found at http://stackoverflow.com/questions/10879658
        String derivedKey = "vphyobtvExGXF7TaOvAkx6CCjHQNYamP2ET8xkhTu-0";
        byte[] z = Base64Url.decode("LfkHot2nGTVlmfxbgxQfMg");  // ByteUtil.randomBytes(16);
        System.out.println(Base64Url.encode(z));
        KdfUtil kdfUtil = new KdfUtil();
        int keyDatalen = 256;
        String alg = ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256;
        byte[] algId = kdfUtil.prependDatalen(StringUtil.getBytesUtf8(alg));
        byte[] partyU = new byte[] {0, 0, 0, 0};
        byte[] partyV = new byte[] {0, 0, 0, 0};
        byte[] pub = ByteUtil.getBytes(keyDatalen);
        byte[] priv = ByteUtil.EMPTY_BYTES;

        ConcatKeyDerivationFunction myConcatKdf = new ConcatKeyDerivationFunction("SHA-256");

        byte[] kdfed = myConcatKdf.kdf(z, keyDatalen, algId, partyU, partyV, pub, priv);
        assertEquals(derivedKey, Base64Url.encode(kdfed));
    }

    public void testKdf3() throws Exception
    {
        // test values produced from implementation found at http://stackoverflow.com/questions/10879658
        String derivedKey = "yRbmmZJpxv3H1aq3FgzESa453frljIaeMz6pt5rQZ4Q5Hs-4RYoFRXFh_qBsbTjlsj8JxIYTWj-cp5LKtgi1fBRsf_5yTEcLDv4pKH2fNxjbEOKuVVDWA1_Qv2IkEC0_QSi3lSSELcJaNX-hDG8occ7oQv-w8lg6lLJjg58kOes";
        byte[] z = Base64Url.decode("KSDnQpf2iurUsAbcuI4YH-FKfk2gecN6cWHTYlBzrd8");
        KdfUtil kdfUtil = new KdfUtil();
        int keyDatalen = 1024;
        String alg = "meh";
        byte[] algId = kdfUtil.prependDatalen(StringUtil.getBytesUtf8(alg));
        byte[] partyU = new byte[] {0, 0, 0, 5, 65, 108, 105, 99, 101};
        byte[] partyV = new byte[] {0, 0, 0, 3, 66, 111, 98};
        byte[] pub = ByteUtil.getBytes(keyDatalen);
        byte[] priv = ByteUtil.EMPTY_BYTES;

        ConcatKeyDerivationFunction myConcatKdf = new ConcatKeyDerivationFunction("SHA-256");

        byte[] kdfed = myConcatKdf.kdf(z, keyDatalen, algId, partyU, partyV, pub, priv);
        assertEquals(derivedKey, Base64Url.encode(kdfed));
    }

    public void testKdf4() throws Exception
    {
        // test values produced from implementation found at http://stackoverflow.com/questions/10879658
        String derivedKey = "SNOvl6h5iSYWJ_EhlnvK8o6om9iyR8HkKMQtQYGkYKkVY0HFMleoUm-H6-kLz8sW";
        byte[] z = Base64Url.decode("zp9Hot2noTVlmfxbkXqfn1");
        KdfUtil kdfUtil = new KdfUtil();
        int keyDatalen = 384;
        String alg = ContentEncryptionAlgorithmIdentifiers.AES_192_CBC_HMAC_SHA_384;
        byte[] algId = kdfUtil.prependDatalen(StringUtil.getBytesUtf8(alg));
        byte[] partyU = new byte[] {0, 0, 0, 0};
        byte[] partyV = new byte[] {0, 0, 0, 0};
        byte[] pub = ByteUtil.getBytes(keyDatalen);
        byte[] priv = ByteUtil.EMPTY_BYTES;

        ConcatKeyDerivationFunction myConcatKdf = new ConcatKeyDerivationFunction("SHA-256");

        byte[] kdfed = myConcatKdf.kdf(z, keyDatalen, algId, partyU, partyV, pub, priv);
        assertEquals(derivedKey, Base64Url.encode(kdfed));
    }
}
