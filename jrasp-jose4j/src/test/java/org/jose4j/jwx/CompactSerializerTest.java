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

package org.jose4j.jwx;

import junit.framework.TestCase;
import org.jose4j.lang.JoseException;

/**
 */
public class CompactSerializerTest extends TestCase
{
    public void testDeserialize1() throws JoseException
    {
        String cs = "one.two.three";
        String[] parts = CompactSerializer.deserialize(cs);
        int i = 0;
        assertEquals("one", parts[i++]);
        assertEquals("two", parts[i++]);
        assertEquals("three", parts[i++]);
        assertEquals(i, parts.length);
    }

    public void testDeserialize2()  throws JoseException
    {
        String cs = "one.two.three.four";
        String[] parts = CompactSerializer.deserialize(cs);
        int i = 0;
        assertEquals("one", parts[i++]);
        assertEquals("two", parts[i++]);
        assertEquals("three", parts[i++]);
        assertEquals("four", parts[i++]);
        assertEquals(i, parts.length);
    }

    public void testDeserialize3() throws JoseException
    {
        String cs = "one.two.";
        String[] parts = CompactSerializer.deserialize(cs);
        int i = 0;
        assertEquals("one", parts[i++]);
        assertEquals("two", parts[i++]);
        assertEquals("", parts[i++]);
        assertEquals(i, parts.length);
    }

    public void testDeserialize4() throws JoseException
    {
        String cs = "one.two.three.";
        String[] parts = CompactSerializer.deserialize(cs);
        int i = 0;
        assertEquals("one", parts[i++]);
        assertEquals("two", parts[i++]);
        assertEquals("three", parts[i++]);
        assertEquals("", parts[i++]);
        assertEquals(i, parts.length);
    }

    public void testDeserialize5() throws JoseException
    {
        String cs = "one..three.four.five";
        String[] parts = CompactSerializer.deserialize(cs);
        int i = 0;
        assertEquals("one", parts[i++]);
        assertEquals("", parts[i++]);
        assertEquals("three", parts[i++]);
        assertEquals("four", parts[i++]);
        assertEquals("five", parts[i++]);
        assertEquals(i, parts.length);
    }

    public void testSerialize1() throws JoseException
    {
        String cs = CompactSerializer.serialize("one", "two", "three");
        assertEquals("one.two.three", cs);
    }

    public void testSerialize2() throws JoseException
    {
        String cs = CompactSerializer.serialize("one", "two", "three", "four");
        assertEquals("one.two.three.four", cs);
    }

    public void testSerialize3() throws JoseException
    {
        String cs = CompactSerializer.serialize("one", "two", "three", null);
        assertEquals("one.two.three.", cs);
    }

    public void testSerialize4() throws JoseException
    {
        String cs = CompactSerializer.serialize("one", "two", "three", "");
        assertEquals("one.two.three.", cs);
    }

    public void testSerialize5() throws JoseException
    {
        String cs = CompactSerializer.serialize("one", null, "three", "four", "five");
        assertEquals("one..three.four.five", cs);
    }

    public void testSerialize6() throws JoseException
    {
        String cs = CompactSerializer.serialize("one", "", "three", "four", "five");
        assertEquals("one..three.four.five", cs);
    }
}
