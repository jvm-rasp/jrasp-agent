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

package org.jose4j.jwk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jose4j.json.JsonUtil;
import org.jose4j.lang.JoseException;

/**
 */
public class JsonWebKeySet {

	public static final String JWK_SET_MEMBER_NAME = "keys";

	private List<JsonWebKey> keys;

	public JsonWebKeySet(String json) throws JoseException {
		Map<String, Object> parsed = JsonUtil.parseJson(json);
		List<Map<String, Object>> jwkParamMapList = (List<Map<String, Object>>) parsed.get(JWK_SET_MEMBER_NAME);

		keys = new ArrayList<JsonWebKey>(jwkParamMapList.size());
		for (Map<String, Object> jwkParamsMap : jwkParamMapList) {
			keys.add(JsonWebKey.Factory.newJwk(jwkParamsMap));
		}
	}

	public JsonWebKeySet(JsonWebKey... keys) {
		this.keys = Arrays.asList(keys);
	}

	public JsonWebKeySet(List<? extends JsonWebKey> keys) {
		this.keys = new ArrayList<JsonWebKey>(keys.size());
		for (JsonWebKey jwk : keys) {
			this.keys.add(jwk);
		}
	}

	public List<JsonWebKey> getJsonWebKeys() {
		return keys;
	}

	public JsonWebKey findJsonWebKey(String keyId, String keyType, String use, String algorithm) {
		List<JsonWebKey> found = findJsonWebKeys(keyId, keyType, use, algorithm);
		return found.isEmpty() ? null : found.iterator().next();
	}

	public List<JsonWebKey> findJsonWebKeys(String keyId, String keyType, String use, String algorithm) {
		List<JsonWebKey> found = new ArrayList<JsonWebKey>();
		for (JsonWebKey jwk : keys) {
			boolean isMeetsCriteria = true;

			if (keyId != null) {
				isMeetsCriteria = keyId.equals(jwk.getKeyId());
			}

			if (use != null) {
				isMeetsCriteria &= use.equals(jwk.getUse());
			}

			if (keyType != null) {
				isMeetsCriteria &= keyType.equals(jwk.getKeyType());
			}

			if (algorithm != null) {
				isMeetsCriteria &= algorithm.equals(jwk.getAlgorithm());
			}

			if (isMeetsCriteria) {
				found.add(jwk);
			}
		}
		return found;
	}

	public String toJson() {
		return toJson(JsonWebKey.OutputControlLevel.INCLUDE_SYMMETRIC);
	}

	public String toJson(JsonWebKey.OutputControlLevel outputControlLevel) {
		LinkedList<Map<String, Object>> keyList = new LinkedList<Map<String, Object>>();

		for (JsonWebKey key : keys) {
			Map<String, Object> params = key.toParams(outputControlLevel);
			keyList.add(params);
		}

		Map<String, Object> jwks = new LinkedHashMap<String, Object>();
		jwks.put(JWK_SET_MEMBER_NAME, keyList);
		return JsonUtil.toJson(jwks);
	}
}
