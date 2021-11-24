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

package org.jose4j.keys;

import org.jose4j.base64url.Base64Url;
import org.jose4j.lang.ByteUtil;

import java.math.BigInteger;

/**
 */
public class BigEndianBigInteger
{
    public static BigInteger fromBytes(byte[] magnitude)
    {
        return new BigInteger(1, magnitude);
    }

    public static BigInteger fromBase64Url(String base64urlEncodedBytes)
    {
        Base64Url base64Url = new Base64Url();
        byte[] magnitude = base64Url.base64UrlDecode(base64urlEncodedBytes);
        return fromBytes(magnitude);
    }
    public static byte[] toByteArray(BigInteger bigInteger, int minArrayLength)
    {
        byte[] bytes = toByteArray(bigInteger);
        if (minArrayLength > bytes.length)
        {
            bytes = ByteUtil.concat(new byte[minArrayLength - bytes.length], bytes);
        }
        return bytes;
    }

    public static byte[] toByteArray(BigInteger bigInteger)
    {
        if (bigInteger.signum() < 0)
        {
            String msg = "Cannot convert negative values to an unsigned magnitude byte array: " + bigInteger;
            throw new IllegalArgumentException(msg);
        }

        byte[] twosComplementBytes = bigInteger.toByteArray();
        byte[] magnitude;

        if ((bigInteger.bitLength() % 8 == 0) && (twosComplementBytes[0] == 0) && twosComplementBytes.length > 1)
        {
            magnitude = ByteUtil.subArray(twosComplementBytes, 1, twosComplementBytes.length - 1);
        }
        else
        {
            magnitude = twosComplementBytes;
        }

        return magnitude;
    }

    public static String toBase64Url(BigInteger bigInteger)
    {
        Base64Url base64Url = new Base64Url();
        byte[] bytes = toByteArray(bigInteger);
        return base64Url.base64UrlEncode(bytes);
    }

    public static String toBase64Url(BigInteger bigInteger, int minByteArrayLength)
    {
        Base64Url base64Url = new Base64Url();
        byte[] bytes = toByteArray(bigInteger, minByteArrayLength);
        return base64Url.base64UrlEncode(bytes);
    }
}
