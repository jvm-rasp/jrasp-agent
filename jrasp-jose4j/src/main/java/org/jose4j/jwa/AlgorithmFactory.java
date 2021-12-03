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

package org.jose4j.jwa;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jose4j.lang.InvalidAlgorithmException;

/**
 */
public class AlgorithmFactory<A extends Algorithm> {

	private final Log log;

	private String parameterName;

	private final Map<String, A> algorithms = new LinkedHashMap<String, A>();

	public AlgorithmFactory(String parameterName, Class<A> type) {
		this.parameterName = parameterName;
		log = LogFactory.getLog(this.getClass() + "->" + type.getSimpleName());
	}

	public A getAlgorithm(String algorithmIdentifier) throws InvalidAlgorithmException {
		A algo = algorithms.get(algorithmIdentifier);

		if (algo == null) {
			throw new InvalidAlgorithmException(algorithmIdentifier + " is an unknown, unsupported or unavailable " + parameterName
					+ " algorithm (not one of " + getSupportedAlgorithms() + ").");
		}

		return algo;
	}

	public boolean isAvailable(String algorithmIdentifier) {
		return algorithms.containsKey(algorithmIdentifier);
	}

	public Set<String> getSupportedAlgorithms() {
		return Collections.unmodifiableSet(algorithms.keySet());
	}

	public void registerAlgorithm(A algorithm) {
		String algId = algorithm.getAlgorithmIdentifier();
		if (algorithm.isAvailable()) {
			algorithms.put(algId, algorithm);
			log.debug(algorithm + " registered for " + parameterName + " algorithm " + algId);
		} else {
			log.debug(algId + " is unavailable so will not be registered for " + parameterName + " algorithms.");
		}
	}

	public void unregisterAlgorithm(String algorithmIdentifier) {
		algorithms.remove(algorithmIdentifier);
	}
}
