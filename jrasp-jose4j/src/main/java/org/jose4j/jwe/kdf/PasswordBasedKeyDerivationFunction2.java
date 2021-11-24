package org.jose4j.jwe.kdf;

import org.jose4j.keys.HmacKey;
import org.jose4j.lang.ByteUtil;
import org.jose4j.lang.InvalidKeyException;
import org.jose4j.lang.UncheckedJoseException;
import org.jose4j.mac.MacUtil;

import javax.crypto.Mac;
import java.io.ByteArrayOutputStream;

/**
 * An implementation of PBKDF2 from RFC 2898 using HMAC as the underlying pseudorandom function.
 */
public class PasswordBasedKeyDerivationFunction2
{
    private String hmacAlgorithm;

    public PasswordBasedKeyDerivationFunction2(String hmacAlgorithm)
    {
        this.hmacAlgorithm = hmacAlgorithm;
    }

    public byte[] derive(byte[] password, byte[] salt, int iterationCount, int dkLen) throws InvalidKeyException
    {
        Mac prf = MacUtil.getInitializedMac(hmacAlgorithm, new HmacKey(password));
        int hLen = prf.getMacLength();

        //  1. If dkLen > (2^32 - 1) * hLen, output "derived key too long" and
        //     stop.
        long maxDerivedKeyLength = 4294967295L; // value of (long) Math.pow(2, 32) - 1;
        if (dkLen > maxDerivedKeyLength)
        {
            throw new UncheckedJoseException("derived key too long " + dkLen);
        }

        //  2. Let l be the number of hLen-octet blocks in the derived key,
        //     rounding up, and let r be the number of octets in the last
        //     block:
        //
        //               l = CEIL (dkLen / hLen) ,
        //               r = dkLen - (l - 1) * hLen .
        //
        //     Here, CEIL (x) is the "ceiling" function, i.e. the smallest
        //     integer greater than, or equal to, x.
        int l = (int) Math.ceil((double) dkLen / (double) hLen);
        int r = dkLen - (l - 1) * hLen;

        //  3. For each block of the derived key apply the function F defined
        //     below to the password P, the salt S, the iteration count c, and
        //     the block index to compute the block:
        //
        //               T_1 = F (P, S, c, 1) ,
        //               T_2 = F (P, S, c, 2) ,
        //               ...
        //               T_l = F (P, S, c, l) ,
        //
        //     where the function F is defined as the exclusive-or sum of the
        //     first c iterates of the underlying pseudorandom function PRF
        //     applied to the password P and the concatenation of the salt S
        //     and the block index i:
        //
        //               F (P, S, c, i) = U_1 \xor U_2 \xor ... \xor U_c
        //
        //     where
        //
        //               U_1 = PRF (P, S || INT (i)) ,
        //               U_2 = PRF (P, U_1) ,
        //               ...
        //               U_c = PRF (P, U_{c-1}) .
        //
        //     Here, INT (i) is a four-octet encoding of the integer i, most
        //     significant octet first.

        //  4. Concatenate the blocks and extract the first dkLen octets to
        //     produce a derived key DK:
        //
        //               DK = T_1 || T_2 ||  ...  || T_l<0..r-1>
        //
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for (int i = 0; i < l; i++)
        {
            byte[] block = f(salt, iterationCount, i + 1, prf);
            if (i == (l - 1))
            {
                block = ByteUtil.subArray(block, 0, r);
            }
            byteArrayOutputStream.write(block, 0, block.length);
        }

        //  5. Output the derived key DK.
        return byteArrayOutputStream.toByteArray();
    }

    byte[] f(byte[] salt, int iterationCount, int blockIndex, Mac prf) throws InvalidKeyException
    {
        byte[] currentU;
        byte[] lastU = null;
        byte[] xorU = null;

        for (int i = 1; i <= iterationCount; i++)
        {
            byte[] inputBytes;
            if (i == 1)
            {
                inputBytes = ByteUtil.concat(salt, ByteUtil.getBytes(blockIndex));
                currentU = prf.doFinal(inputBytes);
                xorU = currentU;
            }
            else
            {
                currentU = prf.doFinal(lastU);
                for (int j = 0; j < currentU.length; j++)
                {
                    xorU[j] = (byte) (currentU[j] ^ xorU[j]);
                }
            }

            lastU = currentU;
        }
        return xorU;
    }
}
