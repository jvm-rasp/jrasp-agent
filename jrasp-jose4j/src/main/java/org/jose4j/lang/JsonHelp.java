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

import org.jose4j.jwt.IntDate;

import java.util.List;
import java.util.Map;

/**
 */
public class JsonHelp
{
    public static String getString(Map<String, Object> map, String name)
    {
        Object object = map.get(name);
        return (String) object;
    }

    public static List<String> getStringArray(Map<String, Object> map, String name)
    {
        Object object = map.get(name);
        return (List<String>) object;
    }

    public static IntDate getIntDate(Map<String, Object> map, String name)
    {
        long l = getLong(map, name);
        return IntDate.fromSeconds(l);
    }

    public static Long getLong(Map<String, ?> map, String name)
    {
        Object o = map.get(name);
        return o != null ? ((Number) o).longValue() : null;
    }
}
