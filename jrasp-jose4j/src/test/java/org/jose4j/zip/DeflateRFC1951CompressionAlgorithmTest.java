package org.jose4j.zip;

import junit.framework.TestCase;
import org.jose4j.base64url.Base64Url;
import org.jose4j.lang.JoseException;
import org.jose4j.lang.StringUtil;

/**
 */
public class DeflateRFC1951CompressionAlgorithmTest extends TestCase
{
    public void testRoundTrip() throws JoseException
    {
        String dataString = "test test test test test test test test test test test test test test test test and stuff";
        byte[] data = StringUtil.getBytesUtf8(dataString);
        CompressionAlgorithm ca = new DeflateRFC1951CompressionAlgorithm();
        byte[] compressed = ca.compress(data);
        assertTrue(data.length > compressed.length);
        byte[] decompress = ca.decompress(compressed);
        String decompressedString = StringUtil.newStringUtf8(decompress);
        assertEquals(dataString, decompressedString);
    }

    public void testSomeDataCompressedElsewhere() throws JoseException
    {
        String s ="q1bKLC5WslLKKCkpKLaK0Y/Rz0wp0EutSMwtyEnVS87PVdLhUkqtKFCyMjQ2NTcyNTW3sACKJJamoGgqRujJL0o" +
                "H6ckqyQSqKMmNLIsMCzWqsPAp8zM3cjINjHdNTPbQizd1BClKTC4CKjICMYtLk4BMp6LMxDylWi4A";
        byte[] decoded = Base64Url.decode(s);
        CompressionAlgorithm ca = new DeflateRFC1951CompressionAlgorithm();
        byte[] decompress = ca.decompress(decoded);
        String decompedString = StringUtil.newStringUtf8(decompress);

        String expected = "{\"iss\":\"https:\\/\\/idp.example.com\",\n" +
                "\"exp\":1357255788,\n" +
                "\"aud\":\"https:\\/\\/sp.example.org\",\n" +
                "\"jti\":\"tmYvYVU2x8LvN72B5Q_EacH._5A\",\n" +
                "\"acr\":\"2\",\n" +
                "\"sub\":\"Brian\"}\n";

        assertEquals(expected, decompedString);
    }

    public void testSomeMoreDataCompressedElsewhere() throws JoseException
    {
        byte[] compressed = new byte[]{-13,72,-51,-55,-55,87,40,-49,47,-54,73,81,84,-16,-96,38,7,0};
        CompressionAlgorithm ca = new DeflateRFC1951CompressionAlgorithm();
        byte[] decompress = ca.decompress(compressed);
        String decompedString = StringUtil.newStringUtf8(decompress);
        assertTrue(decompedString.contains("Hello world!"));
    }
}
