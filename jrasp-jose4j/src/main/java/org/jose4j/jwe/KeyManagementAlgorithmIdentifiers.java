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

package org.jose4j.jwe;

/**
 */
public class KeyManagementAlgorithmIdentifiers
{
    public static final String RSA1_5 = "RSA1_5";
    public static final String RSA_OAEP = "RSA-OAEP";
    public static final String RSA_OAEP_256 = "RSA-OAEP-256";

    public static final String ECDH_ES = "ECDH-ES";
    public static final String ECDH_ES_A128KW = "ECDH-ES+A128KW";
    public static final String ECDH_ES_A192KW = "ECDH-ES+A192KW";
    public static final String ECDH_ES_A256KW = "ECDH-ES+A256KW";

    public static final String A128KW = "A128KW";
    public static final String A192KW = "A192KW";
    public static final String A256KW = "A256KW";

    public static final String A128GCMKW = "A128GCMKW";
    public static final String A192GCMKW = "A192GCMKW";
    public static final String A256GCMKW = "A256GCMKW";

    public static final String PBES2_HS256_A128KW = "PBES2-HS256+A128KW";
    public static final String PBES2_HS384_A192KW = "PBES2-HS384+A192KW";
    public static final String PBES2_HS512_A256KW = "PBES2-HS512+A256KW";

    public static final String DIRECT = "dir";
}
