package org.jose4j.lang;

/**
 */
public class InvalidKeyException extends JoseException
{
    public InvalidKeyException(String message)
    {
        super(message);
    }

    public InvalidKeyException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
