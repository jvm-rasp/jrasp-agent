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
public class ContentEncryptionAlgorithmIdentifiers
{
    public static final String AES_128_CBC_HMAC_SHA_256 = "A128CBC-HS256";
    public static final String AES_192_CBC_HMAC_SHA_384 = "A192CBC-HS384";
    public static final String AES_256_CBC_HMAC_SHA_512 = "A256CBC-HS512";

    public static final String AES_128_GCM = "A128GCM";
    public static final String AES_192_GCM = "A192GCM";
    public static final String AES_256_GCM = "A256GCM";
}
