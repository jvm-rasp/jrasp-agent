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

import java.io.UnsupportedEncodingException;

/**
 */
public class StringUtil
{
    public static final String UTF_8 = "UTF-8";
    public static final String US_ASCII = "US-ASCII";

    public static String newStringUtf8(byte[] bytes)
    {
        return newString(bytes, UTF_8);
    }

    public static String newStringUsAscii(byte[] bytes)
    {
        return newString(bytes, US_ASCII);
    }

    public static String newString(byte[] bytes, String charsetName)
    {
        try
        {
            return (bytes == null) ? null : new String(bytes, charsetName);
        }
        catch (UnsupportedEncodingException e)
        {
            throw newISE(charsetName);
        }
    }

    public static byte[] getBytesUtf8(String string)
    {
        return getBytesUnchecked(string, UTF_8);
    }

    public static byte[] getBytesAscii(String string)
    {
        return getBytesUnchecked(string, US_ASCII);
    }

    public static byte[] getBytesUnchecked(String string, String charsetName)
    {
        try
        {
            return (string == null) ? null : string.getBytes(charsetName);
        }
        catch (UnsupportedEncodingException e)
        {
            throw newISE(charsetName);
        }
    }

    private static IllegalStateException newISE(String charsetName)
    {
        return new IllegalStateException("Unknown or unsupported character set name: " + charsetName);
    }
}
// todo look at StandardCharsets (new in java 7) to maybe simplify the unchecked stuff with known charsets
