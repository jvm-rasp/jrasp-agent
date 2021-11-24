package org.jose4j.jwe.kdf;

import org.jose4j.lang.StringUtil;
import org.junit.Assert;
import org.junit.Test;


/**
 * Tests from https://tools.ietf.org/html/rfc6070 which,
 * "contains test vectors for the Public-Key Cryptography
 *  Standards (PKCS) #5 Password-Based Key Derivation Function 2 (PBKDF2)
 *  with the Hash-based Message Authentication Code (HMAC) Secure Hash
 *  Algorithm (SHA-1) pseudorandom function."
 */
public class Rfc6070Pbkdf2TestVectorsTest
{
    @Test
    public void doRfc6070Test1() throws Exception
    {
        String p = "password";
        String s = "salt";
        int c = 1;
        int dkLen = 20;
        byte[] expectedOutput = new byte[] {12, 96, -56, 15, -106, 31, 14, 113, -13, -87, -75, 36, -81, 96, 18, 6, 47, -32, 55, -90};
        testAndCompare(p, s, c, dkLen, expectedOutput);
    }

    @Test
    public void doRfc6070Test2() throws Exception
    {
        String p = "password";
        String s = "salt";
        int c = 2;
        int dkLen = 20;
        byte[] expectedOutput = new byte[] {-22, 108, 1, 77, -57, 45, 111, -116, -51, 30, -39, 42, -50, 29, 65, -16, -40, -34, -119, 87};
        testAndCompare(p, s, c, dkLen, expectedOutput);
    }

    @Test
    public void doRfc6070Test3() throws Exception
    {
        String p = "password";
        String s = "salt";
        int c = 4096;
        int dkLen = 20;
        byte[] expectedOutput = new byte[] {75, 0, 121, 1, -73, 101, 72, -102, -66, -83, 73, -39, 38, -9, 33, -48, 101, -92, 41, -63};
        testAndCompare(p, s, c, dkLen, expectedOutput);
    }

    //@Test  // this one takes too darn long to run b/c of the iteration count so don't run it normally
    public void doRfc6070Test4() throws Exception
    {
        String p = "password";
        String s = "salt";
        int c = 16777216;
        int dkLen = 20;
        byte[] expectedOutput = new byte[] {-18, -2, 61, 97, -51, 77, -92, -28, -23, -108, 91, 61, 107, -94, 21, -116, 38, 52, -23, -124};
        testAndCompare(p, s, c, dkLen, expectedOutput);
    }

    @Test
    public void doRfc6070Test5() throws Exception
    {

        String p = "passwordPASSWORDpassword";
        String s = "saltSALTsaltSALTsaltSALTsaltSALTsalt";
        int c = 4096;
        int dkLen = 25;
        byte[] expectedOutput = new byte[] {61, 46, -20, 79, -28, 28, -124, -101, -128, -56, -40, 54, 98, -64, -28, 74, -117, 41, 26, -106, 76, -14, -16, 112, 56};
        testAndCompare(p, s, c, dkLen, expectedOutput);
    }

    @Test
    public void doRfc6070Test6() throws Exception
    {
        String p = "pass\0word";
        String s = "sa\0lt";
        int c = 4096;
        int dkLen = 16;
        byte[] expectedOutput = new byte[] {86, -6, 106, -89, 85, 72, 9, -99, -52, 55, -41, -16, 52, 37, -32, -61};
        testAndCompare(p, s, c, dkLen, expectedOutput);
    }

    void testAndCompare(String p, String s, int c, int dkLen, byte[] expectedOutput) throws Exception
    {
        PasswordBasedKeyDerivationFunction2 pbkdf2 = new PasswordBasedKeyDerivationFunction2("HmacSHA1");
        byte[] derived = pbkdf2.derive(StringUtil.getBytesUtf8(p), StringUtil.getBytesUtf8(s), c, dkLen);
        Assert.assertArrayEquals(expectedOutput, derived);
    }
}
