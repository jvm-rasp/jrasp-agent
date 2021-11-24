package org.jose4j.jwk;

import junit.framework.TestCase;
import org.jose4j.lang.ByteUtil;

import javax.crypto.SecretKey;

/**
 */
public class OctJwkGeneratorTest extends TestCase
{
    public void testGen()
    {
        for (int size : new int[]{128, 192, 256, 192, 384, 512})
        {
            OctetSequenceJsonWebKey jsonWebKey = OctJwkGenerator.generateJwk(size);
            assertNotNull(jsonWebKey.getKey());
            assertTrue(jsonWebKey.getKey() instanceof SecretKey);
            assertEquals(ByteUtil.byteLength(size), jsonWebKey.getOctetSequence().length);
        }
    }
}
