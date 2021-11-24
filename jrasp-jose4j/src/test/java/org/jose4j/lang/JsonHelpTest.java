/*
 * Copyright 2012-2014 Brian Campbell
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

import org.jose4j.json.JsonUtil;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 */
public class JsonHelpTest
{
    @Test
    public void longs() throws JoseException
    {
        Map<String,Object> map = JsonUtil.parseJson("{\"number\":1024}");
        Long nope = JsonHelp.getLong(map, "nope");
        assertThat(nope, is(nullValue()));
        Long number = JsonHelp.getLong(map, "number");
        assertThat(number, is(equalTo(1024L)));
    }
}
