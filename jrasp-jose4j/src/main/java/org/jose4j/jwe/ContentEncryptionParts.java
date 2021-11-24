package org.jose4j.jwe;

/**
*/
public class ContentEncryptionParts
{
    private byte[] iv;
    private byte[] ciphertext;
    private byte[] authenticationTag;

    public ContentEncryptionParts(byte[] iv, byte[] ciphertext, byte[] authenticationTag)
    {
        this.iv = iv;
        this.ciphertext = ciphertext;
        this.authenticationTag = authenticationTag;
    }

    public byte[] getIv()
    {
        return iv;
    }

    public byte[] getCiphertext()
    {
        return ciphertext;
    }

    public byte[] getAuthenticationTag()
    {
        return authenticationTag;
    }
}
