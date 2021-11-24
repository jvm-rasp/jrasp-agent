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

import static org.jose4j.jwa.AlgorithmConstraints.ConstraintType.BLACKLIST;
import static org.jose4j.jwa.AlgorithmConstraints.ConstraintType.WHITELIST;
import static org.jose4j.jws.AlgorithmIdentifiers.NONE;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jose4j.lang.InvalidAlgorithmException;

/**
 */
public class AlgorithmConstraints {

	public static final AlgorithmConstraints NO_CONSTRAINTS = new AlgorithmConstraints(ConstraintType.BLACKLIST);

	public static final AlgorithmConstraints DISALLOW_NONE = new AlgorithmConstraints(BLACKLIST, NONE);

	public static final AlgorithmConstraints ALLOW_ONLY_NONE = new AlgorithmConstraints(WHITELIST, NONE);

	public enum ConstraintType {
		WHITELIST, BLACKLIST
	}

	private final ConstraintType type;

	private final Set<String> algorithms;

	public AlgorithmConstraints(ConstraintType type, String... algorithms) {
		if (type == null) {
			throw new NullPointerException("ConstraintType cannot be null");
		}
		this.type = type;
		this.algorithms = new HashSet<String>(Arrays.asList(algorithms));
	}

	public void checkConstraint(String algorithm) throws InvalidAlgorithmException {
		switch (type) {
		case WHITELIST:
			if (!algorithms.contains(algorithm)) {
				throw new InvalidAlgorithmException("'" + algorithm + "' is not a whitelisted algorithm.");
			}
			break;
		case BLACKLIST:
			if (algorithms.contains(algorithm)) {
				throw new InvalidAlgorithmException("'" + algorithm + "' is a blacklisted algorithm.");
			}
			break;
		}
	}
}
