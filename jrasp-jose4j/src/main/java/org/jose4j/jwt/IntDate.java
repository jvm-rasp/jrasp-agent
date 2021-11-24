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

package org.jose4j.jwt;

import java.util.Date;

/**
 */
public class IntDate
{
    private long value;
    private static final long CONVERSION = 1000L;

    private IntDate(long value)
    {
        this.value = value;
    }

    public static IntDate now()
    {
        return fromMillis(System.currentTimeMillis());
    }

    public static IntDate fromSeconds(long secondsFromEpoch)
    {
        return new IntDate(secondsFromEpoch);
    }

    public static IntDate fromMillis(long millisecondsFromEpoch)
    {
        return fromSeconds(millisecondsFromEpoch / CONVERSION);
    }

    public void addSeconds(long seconds)
    {
        value += seconds;
    }

    /**
     * Retruns a numeric value representing the number of seconds from
     * 1970-01-01T0:0:0Z UTC until the given UTC date/time
     * @return value
     */
    public long getValue()
    {
        return value;
    }

    public long getValueInMillis()
    {
        return getValue() * CONVERSION;  
    }

    public boolean before(IntDate when)
    {
        return value < when.getValue();
    }

    public boolean after(IntDate when)
    {
        return value > when.getValue();
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("IntDate");
        sb.append("{").append(getValue()).append(" --> ");
        sb.append(new Date(getValueInMillis()));
        sb.append('}');                           
        return sb.toString();
    }

    @Override
    public boolean equals(Object other)
    {
        return (this == other) || ((other instanceof IntDate) && (value == ((IntDate) other).value));
    }

    @Override
    public int hashCode()
    {
        return (int) (value ^ (value >>> 32));
    }
}
