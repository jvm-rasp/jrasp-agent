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

package org.jose4j.json;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jose4j.json.internal.json_simple.JSONValue;
import org.jose4j.json.internal.json_simple.parser.ContainerFactory;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.lang.JoseException;

/**
 * This class should be the point of contact for JSON processing.
 * 
 * The code in the internal.json_simple package was originally taken from the
 * Apache 2.0 licensed json-simple source code (changing the package). I'm a
 * little uneasy about it. But json-simple hasn't changed much in years and
 * jose4j only needs fairly basic JSON processing. Doing this lets me remove one
 * more dependency and avoid any potential dependency conflicts. It also will
 * let me to make changes to the JSON processing like not escaping forward
 * slashes. There’s some risk in this but moving to a new/different processor
 * in the future isn’t really made particularly more difficult by this (as
 * long as this class is the touch point for JSON processing).
 */
public class JsonUtil {

	private static final ContainerFactory CONTAINER_FACTORY = new ContainerFactory() {

		public List creatArrayContainer() {
			return new ArrayList<Object>();
		}

		public Map createObjectContainer() {
			return new DupeKeyDisallowingLinkedHashMap();
		}
	};

	public static Map<String, Object> parseJson(String jsonString) throws JoseException {
		try {
			JSONParser parser = new JSONParser();
			return (DupeKeyDisallowingLinkedHashMap) parser.parse(jsonString, CONTAINER_FACTORY);
		} catch (Exception e) {
			throw new JoseException("Parsing error: " + e, e);
		}
	}

	public static String toJson(Map<String, ?> map) {
		return JSONValue.toJSONString(map);
	}

	public static void writeJson(Map<String, ?> map, Writer w) throws IOException {
		JSONValue.writeJSONString(map, w);
	}

	static class DupeKeyDisallowingLinkedHashMap extends LinkedHashMap<String, Object> {

		@Override
		public Object put(String key, Object value) {
			if (this.containsKey(key)) {
				throw new IllegalArgumentException("An entry for '" + key + "' already exists. Parameter names must be unique.");
			}

			return super.put(key, value);
		}
	}
}
