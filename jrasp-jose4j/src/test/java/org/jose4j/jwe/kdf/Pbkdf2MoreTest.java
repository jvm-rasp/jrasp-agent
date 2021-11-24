package org.jose4j.jwe.kdf;

import org.jose4j.base64url.Base64Url;
import org.jose4j.lang.InvalidKeyException;
import org.jose4j.lang.StringUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 *  Some PBKDF2 tests against keys derived using a reliable 3rd party implementation
 */
public class Pbkdf2MoreTest
{
    @Test
    public void test1() throws InvalidKeyException
    {
        int ic = 1024;
        String encodedSalt = "_bdWuYq60PU";
        int dklenBytes = 16;
        String pwd = "password7";
        String prn = "HmacSHA256";
        String pbk = "uDd04RmfZgf4u-ajXdPhwA";
        testIt(ic, encodedSalt, dklenBytes, pwd, prn, pbk);
    }

    @Test
    public void test2() throws InvalidKeyException
    {
        int ic = 500;
        String encodedSalt = "4qJnWHair2GDKxXd9SYE64MA";
        int dklenBytes = 64;
        String pwd = "passpass";
        String prn = "HmacSHA256";
        String pbk = "zEZlBzGg2LkthRoJHApI7chEuQuQ57uTDWIhEUw-VR6eq7rQ4ETLYeVy_8nCJUCJPmzCZ2WmNtP-fUfF3YzDHw";
        testIt(ic, encodedSalt, dklenBytes, pwd, prn, pbk);
    }

    @Test
    public void test3() throws InvalidKeyException
    {
        int ic = 7;
        String encodedSalt = "SCZwvZ_lZek";
        int dklenBytes = 32;
        String pwd = "passthattherepass";
        String prn = "HmacSHA384";
        String pbk = "_uNqQq9PjSmsAmTnnz0fGM4d2noW4JrVCNNiE4yxf4M";
        testIt(ic, encodedSalt, dklenBytes, pwd, prn, pbk);
    }

    @Test
    public void test4() throws InvalidKeyException
    {
        int ic = 20;
        String encodedSalt = "eGOROhJ6jDqos0hYhQh8EYfGJ7g";
        int dklenBytes = 32;
        String pwd = "blahblah";
        String prn = "HmacSHA512";
        String pbk = "24s7jqUazZ6QHmkU5UyyLw22zeSK87bEmAeugxDDYM4";
        testIt(ic, encodedSalt, dklenBytes, pwd, prn, pbk);
    }

    @Test
    public void test5() throws InvalidKeyException
    {
        int ic = 1;
        String encodedSalt = "WKSJ8q-EvvyP-0RQd6g";
        int dklenBytes = 16;
        String pwd = "blahblahblahblah";
        String prn = "HmacSHA256";
        String pbk = "6a1-B_PrQu-Pfi9-6w_Y5A";
        testIt(ic, encodedSalt, dklenBytes, pwd, prn, pbk);
    }

    @Test
    public void test6() throws InvalidKeyException
    {
        int ic = 3;
        String encodedSalt = "SldHVNgHJadJ";
        int dklenBytes = 128;
        String pwd = "dabears";
        String prn = "HmacSHA256";
        String pbk = "nperkSKKFADfulz5xpNkvBrbLK6z075ZUgssE72EWY0vbijZo1rT8pyBhS-hHLcXJi03LXb0E8383sIYjsZInH5OupD" +
                "4dLWXLiE4ZTB1HV8dESTwQug_M7EqVKqIbGW2HV2k5CQUfN2cK9V1U3Jmi0oEJps2fS12jXlMqbNA--Y";
        testIt(ic, encodedSalt, dklenBytes, pwd, prn, pbk);

    }

    private void testIt(int count, String salt, int dklenBytes, String pwd, String prn, String pbk) throws InvalidKeyException
    {
        PasswordBasedKeyDerivationFunction2 pbkdf2 = new PasswordBasedKeyDerivationFunction2(prn);
        byte[] pwdBytes = StringUtil.getBytesUtf8(pwd);
        byte[] derivedKey = pbkdf2.derive(pwdBytes, Base64Url.decode(salt), count, dklenBytes);
        Assert.assertEquals(pbk, Base64Url.encode(derivedKey));
    }
}
