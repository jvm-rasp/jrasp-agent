package org.jose4j.keys;

import org.jose4j.lang.JoseException;

import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Set;

/**
 */
abstract class KeyPairUtil
{
    abstract String getAlgorithm();

    protected KeyFactory getKeyFactory() throws JoseException
    {
        try
        {
            return KeyFactory.getInstance(getAlgorithm());
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new JoseException("Couldn't find " + getAlgorithm() + " KeyFactory! " + e, e);
        }
    }

    protected KeyPairGenerator getKeyPairGenerator() throws JoseException
    {
        try
        {
            return KeyPairGenerator.getInstance(getAlgorithm());
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new JoseException("Couldn't find " + getAlgorithm() + " KeyPairGenerator! " + e, e);
        }
    }

    public boolean isAvailable()
    {
        Set<String> keyFactories = Security.getAlgorithms("KeyFactory");
        Set<String> keyPairGenerators = Security.getAlgorithms("KeyPairGenerator");
        String algorithm = getAlgorithm();
        return keyPairGenerators.contains(algorithm) && keyFactories.contains(algorithm);
    }
}
