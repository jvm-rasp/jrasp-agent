package org.jose4j.jwe;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jose4j.lang.ByteUtil;

import javax.crypto.Cipher;
import java.security.NoSuchAlgorithmException;

/**
 */
public class CipherStrengthSupport
{
    private static Log log = LogFactory.getLog(CipherStrengthSupport.class);

    public static boolean isAvailable(String algorithm, int keyByteLength)
    {
        boolean isAvailable;
        int bitKeyLength = ByteUtil.bitLength(keyByteLength);
        try
        {
            int maxAllowedKeyLength = Cipher.getMaxAllowedKeyLength(algorithm);
            isAvailable = (bitKeyLength <= maxAllowedKeyLength);

            if (!isAvailable)
            {
                log.debug("max allowed key length for " + algorithm + " is " + maxAllowedKeyLength);
            }
        }
        catch (NoSuchAlgorithmException e)
        {
            log.debug("Unknown/unsupported algorithm," + algorithm + " " + e);
            isAvailable = false;
        }
        return isAvailable;
    }

}
