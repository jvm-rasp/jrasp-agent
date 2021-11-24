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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jose4j.keys.KeyPersuasion;

/**
 */
public abstract class AlgorithmInfo implements Algorithm
{
    protected final Log log = LogFactory.getLog(this.getClass());

    private String algorithmIdentifier;
    private String javaAlgorithm;
    private KeyPersuasion keyPersuasion;
    private String keyType;

    public void setAlgorithmIdentifier(String algorithmIdentifier)
    {
        this.algorithmIdentifier = algorithmIdentifier;
    }

    public void setJavaAlgorithm(String javaAlgorithm)
    {
        this.javaAlgorithm = javaAlgorithm;
    }

    public String getJavaAlgorithm()
    {
        return javaAlgorithm;
    }

    public String getAlgorithmIdentifier()
    {
        return algorithmIdentifier;
    }

    public KeyPersuasion getKeyPersuasion()
    {
        return keyPersuasion;
    }

    public void setKeyPersuasion(KeyPersuasion keyPersuasion)
    {
        this.keyPersuasion = keyPersuasion;
    }

    public String getKeyType()
    {
        return keyType;
    }

    public void setKeyType(String keyType)
    {
        this.keyType = keyType;
    }
}
