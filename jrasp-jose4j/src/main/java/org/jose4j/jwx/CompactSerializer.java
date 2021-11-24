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

import org.jose4j.lang.JoseException;

/**
 */
public class CompactSerializer
{
    private static final String PERIOD_SEPARATOR = ".";
    private static final String PERIOD_SEPARATOR_REGEX = "\\.";

    private static final String EMPTY_STRING = "";

    public static String[] deserialize(String compactSerialization)
    {
        String[] parts = compactSerialization.split(PERIOD_SEPARATOR_REGEX);

        if (compactSerialization.endsWith(PERIOD_SEPARATOR))
        {
            String[] tempParts = new String[parts.length + 1];
            System.arraycopy(parts, 0, tempParts, 0, parts.length);
            tempParts[parts.length] = EMPTY_STRING;
            parts = tempParts;
        }

        return parts;
    }

    public static String serialize(String... parts) throws JoseException
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++)
        {
            String part = (parts[i] == null) ? EMPTY_STRING : parts[i];
            sb.append(part);
            if (i != parts.length - 1)
            {
                sb.append(PERIOD_SEPARATOR);
            }
        }
        return sb.toString();
    }
}
