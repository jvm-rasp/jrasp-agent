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

package org.jose4j.base64url;

import org.jose4j.base64url.internal.apache.commons.codec.binary.Base64;
import org.jose4j.lang.StringUtil;

/**
 */
public class Base64Url
{
    private Base64 base64urlCodec;

    public Base64Url()
    {
        this.base64urlCodec = new Base64(-1, null, true);
    }

    public String base64UrlDecodeToUtf8String(String encodedValue)
    {
        return base64UrlDecodeToString(encodedValue, StringUtil.UTF_8);
    }

    public String base64UrlDecodeToString(String encodedValue, String charsetName)
    {
        byte[] bytes = base64UrlDecode(encodedValue);
        return StringUtil.newString(bytes, charsetName);
    }

    public byte[] base64UrlDecode(String encodedValue)
    {
        return base64urlCodec.decode(encodedValue);
    }

    public String base64UrlEncodeUtf8ByteRepresentation(String value)
    {
        return base64UrlEncode(value, StringUtil.UTF_8);
    }

    public String base64UrlEncode(String value, String charsetName)
    {
        byte[] bytes = StringUtil.getBytesUnchecked(value, charsetName);
        return base64UrlEncode(bytes);
    }

    public String base64UrlEncode(byte[] bytes)
    {
        return base64urlCodec.encodeToString(bytes);
    }

    private static Base64Url getOne()
    {
        return new Base64Url();
    }

    public static String decodeToUtf8String(String encodedValue)
    {
        return getOne().base64UrlDecodeToString(encodedValue, StringUtil.UTF_8);
    }

    public static String decodeToString(String encodedValue, String charsetName)
    {
        return getOne().base64UrlDecodeToString(encodedValue, charsetName);
    }

    public static byte[] decode(String encodedValue)
    {
        return getOne().base64UrlDecode(encodedValue);
    }

    public static String encodeUtf8ByteRepresentation(String value)
    {
        return getOne().base64UrlEncodeUtf8ByteRepresentation(value);
    }

    public static String encode(String value, String charsetName)
    {
        return getOne().base64UrlEncode(value, charsetName);
    }

    public static String encode(byte[] bytes)
    {
        return getOne().base64UrlEncode(bytes);
    }
}
