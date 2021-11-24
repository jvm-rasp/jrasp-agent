package org.jose4j.jwa;

import org.jose4j.lang.InvalidAlgorithmException;
import org.junit.Test;

import static org.jose4j.jwa.AlgorithmConstraints.ConstraintType.BLACKLIST;
import static org.jose4j.jwa.AlgorithmConstraints.ConstraintType.WHITELIST;
import static org.jose4j.jwe.KeyManagementAlgorithmIdentifiers.*;
import static org.jose4j.jws.AlgorithmIdentifiers.*;

/**
 */
public class AlgorithmConstraintsTest
{
    @Test
    public void blacklist1() throws InvalidAlgorithmException
    {
        AlgorithmConstraints constraints = new AlgorithmConstraints(BLACKLIST, "bad", "badder");
        constraints.checkConstraint("good");
    }

    @Test(expected = InvalidAlgorithmException.class)
    public void blacklist2() throws InvalidAlgorithmException
    {
        AlgorithmConstraints constraints = new AlgorithmConstraints(BLACKLIST, "bad", "badder");
        constraints.checkConstraint("bad");
    }

    @Test(expected = InvalidAlgorithmException.class)
    public void blacklist3() throws InvalidAlgorithmException
    {
        AlgorithmConstraints constraints = new AlgorithmConstraints(BLACKLIST, "bad", "badder");
        constraints.checkConstraint("badder");
    }

    @Test(expected = InvalidAlgorithmException.class)
    public void blacklistNone() throws InvalidAlgorithmException
    {
        AlgorithmConstraints constraints = new AlgorithmConstraints(BLACKLIST, NONE);
        constraints.checkConstraint(NONE);
    }

    @Test(expected = InvalidAlgorithmException.class)
    public void whitelist1() throws InvalidAlgorithmException
    {
        AlgorithmConstraints constraints = new AlgorithmConstraints(WHITELIST, "good", "gooder", "goodest");
        constraints.checkConstraint("bad");
    }

    @Test(expected = InvalidAlgorithmException.class)
    public void whitelist2() throws InvalidAlgorithmException
    {
        AlgorithmConstraints constraints = new AlgorithmConstraints(WHITELIST, "good", "gooder", "goodest");
        constraints.checkConstraint("also bad");
    }

    @Test
    public void whitelist3() throws InvalidAlgorithmException
    {
        AlgorithmConstraints constraints = new AlgorithmConstraints(WHITELIST, "good", "gooder", "goodest");
        constraints.checkConstraint("good");
        constraints.checkConstraint("gooder");
        constraints.checkConstraint("goodest");
    }

    @Test
    public void noRestrictions() throws InvalidAlgorithmException
    {
        AlgorithmConstraints constraints = AlgorithmConstraints.NO_CONSTRAINTS;

        String[] algs = {NONE, HMAC_SHA256, HMAC_SHA512, RSA_USING_SHA256, RSA_USING_SHA512,
                         ECDSA_USING_P256_CURVE_AND_SHA256, "something", A128KW, A256KW,
                         DIRECT, "etc,", "etc."};
        for (String alg : algs)
        {
            constraints.checkConstraint(alg);
        }
    }

}
