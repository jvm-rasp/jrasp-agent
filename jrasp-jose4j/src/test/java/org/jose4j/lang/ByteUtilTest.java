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

package org.jose4j.lang;

import junit.framework.TestCase;

import java.util.Arrays;

/**
 */
public class ByteUtilTest extends TestCase
{
    public void testLeftRight()
    {
        byte[] fullCekBytes = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        byte[] hmacKeyBytes = new byte[fullCekBytes.length/2];
        byte[] encKeyBytes = new byte[fullCekBytes.length/2];
        System.arraycopy(fullCekBytes, 0, hmacKeyBytes, 0, hmacKeyBytes.length);
        System.arraycopy(fullCekBytes, hmacKeyBytes.length, encKeyBytes, 0, encKeyBytes.length);

        byte[] left = ByteUtil.leftHalf(fullCekBytes);
        byte[] right = ByteUtil.rightHalf(fullCekBytes);
        assertTrue(Arrays.equals(hmacKeyBytes, left));
        assertTrue(Arrays.equals(encKeyBytes, right));
    }

    public void testGetBytesLong()
    {
        // http://tools.ietf.org/html/draft-ietf-jose-json-web-encryption-13#appendix-B.3
        long value = 408;
        byte[] bytes = ByteUtil.getBytes(value);
        int[] integers = ByteUtil.convertSignedTwosCompToUnsigned(bytes);
        assertEquals(8, integers.length);
        for (int i = 0 ; i < 6 ; i++)
        {
            assertEquals(0, integers[i]);
        }

        assertEquals(1, integers[6]);
        assertEquals(152, integers[7]);
    }


    public void testConcat1()
    {
        byte[] first = new byte[2];
        byte[] second = new byte[10];
        byte[] third = new byte[15];

        byte[] result = ByteUtil.concat(first, second, third);

        assertEquals(first.length + second.length + third.length, result.length);

        assertTrue(Arrays.equals(new byte[result.length], result));
    }

    public void testConcat2()
    {
        byte[] first = new byte[] {1, 2, 7};
        byte[] second = new byte[] {38, 101};
        byte[] third = new byte[] {5 , 6, 7};

        byte[] result = ByteUtil.concat(first, second, third);

        assertEquals(first.length + second.length + third.length, result.length);

        assertTrue(Arrays.equals(new byte[] {1, 2, 7, 38, 101, 5, 6, 7} , result));
    }

    public void testConcat3()
    {
        byte[] first = new byte[] {1, 2, 7};
        byte[] second = new byte[] {};
        byte[] third = new byte[] {5 , 6, 7};
        byte[] fourth = new byte[] {};

        byte[] result = ByteUtil.concat(first, second, third);

        assertEquals(first.length + second.length + third.length + fourth.length, result.length);

        assertTrue(Arrays.equals(new byte[] {1, 2, 7, 5, 6, 7} , result));
    }

    public void testGetBytesOne()
    {
        byte[] bytes = ByteUtil.getBytes(1);
        assertEquals(4, bytes.length);
        assertEquals(0, bytes[0]);
        assertEquals(0, bytes[1]);
        assertEquals(0, bytes[2]);
        assertEquals(1, bytes[3]);
    }

    public void testGetBytesTwo()
    {
        byte[] bytes = ByteUtil.getBytes(2);
        assertEquals(4, bytes.length);
        assertEquals(0, bytes[0]);
        assertEquals(0, bytes[1]);
        assertEquals(0, bytes[2]);
        assertEquals(2, bytes[3]);
    }

    public void testGetBytesMax()
    {
        byte[] bytes = ByteUtil.getBytes(Integer.MAX_VALUE);
        assertEquals(4, bytes.length);
    }

    public void testConvert() throws JoseException
    {
        for (int i = 0; i < 256; i++)
        {
            byte b = ByteUtil.getByte(i);
            int anInt = ByteUtil.getInt(b);
            assertEquals(i, anInt);
        }
    }

    public void testConvert2() throws JoseException
    {
        boolean keepGoing = true;
        for (byte b = Byte.MIN_VALUE; keepGoing; b++)
        {
            int i = ByteUtil.getInt(b);
            byte aByte = ByteUtil.getByte(i);
            assertEquals(b, aByte);
            if (b == Byte.MAX_VALUE)
            {
                keepGoing = false;
            }
        }
    }

    public void testEquals0()
    {
        byte[] bytes1 = ByteUtil.randomBytes(32);
        byte[] bytes2 = new byte[bytes1.length];
        bytes1[0] = 1;
        compareTest(bytes1, bytes2, false);
        System.arraycopy(bytes1, 0, bytes2, 0, bytes1.length);
        compareTest(bytes1, bytes2, true);
    }

    public void testEquals1()
    {
        compareTest(new byte[]{-1}, new byte[]{1}, false);
    }

    public void testEquals2()
    {
        compareTest("good", "good", true);
    }

    public void testEquals3()
    {
        compareTest("baad", "good", false);
    }

    public void testEquals3b()
    {
        compareTest("bad", "good", false);
    }

    public void testEquals4()
    {
        compareTest("", "niner", false);
    }

    public void testEquals5()
    {
        compareTest("foo", "bar", false);
    }

    public void testEquals6()
    {
        compareTest(new byte[]{-1, 123, 7, 1}, new byte[]{-1, 123, 7, 1}, true);
    }

    public void testEquals7()
    {
        compareTest(new byte[]{-1, 123, -19, 1}, new byte[]{-1, 123, 7, 1}, false);
    }

    public void testEquals8()
    {
        compareTest(new byte[]{-1, 123, 7, 1, -32}, new byte[]{-1, 123, 7, 1}, false);
    }

    public void testEquals9()
    {
        compareTest(new byte[]{-1, 123, 7, 1}, new byte[]{-1, 123, 7, 1, 0}, false);
    }

    public void testEquals10()
    {
        compareTest(null, new byte[]{-1, 123, 7, 1, 0}, false);
    }

    public void testEquals11()
    {
        compareTest(new byte[]{-1, 123, 7, 1}, null, false);
    }

    public void testEquals12()
    {
        compareTest(new byte[0], new byte[]{-1, 123, 7, 1, 0}, false);
    }

    public void testEquals13()
    {
        compareTest(new byte[]{-1, 123, 7, 1}, new byte[0], false);
    }

    public void testEquals14()
    {
        compareTest(new byte[0], new byte[0], true);
    }

    public void testEquals15()
    {
        compareTest((byte [])null, null, true); 
    }

    private void compareTest(String first, String second, boolean shouldMatch)
    {
        compareTest(StringUtil.getBytesUtf8(first), StringUtil.getBytesUtf8(second), shouldMatch);
    }

    private void compareTest(byte[] first, byte[] second, boolean shouldMatch)
    {
        assertEquals(shouldMatch, ByteUtil.secureEquals(first,second));
    }

}
