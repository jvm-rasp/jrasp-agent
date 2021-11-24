package org.jose4j.jwe;

import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwx.KeyValidationSupport;
import org.jose4j.keys.AesKey;
import org.jose4j.keys.KeyPersuasion;
import org.jose4j.lang.ExceptionHelp;
import org.jose4j.lang.InvalidKeyException;
import org.jose4j.lang.JoseException;

import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.security.Key;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.MGF1ParameterSpec;

/**
 */
public class RsaKeyManagementAlgorithm extends WrappingKeyManagementAlgorithm implements KeyManagementAlgorithm
{
    public RsaKeyManagementAlgorithm(String javaAlg, String alg)
    {
        super(javaAlg, alg);
        setKeyType(RsaJsonWebKey.KEY_TYPE);
        setKeyPersuasion(KeyPersuasion.ASYMMETRIC);
    }

    @Override
    public void validateEncryptionKey(Key managementKey, ContentEncryptionAlgorithm contentEncryptionAlg) throws InvalidKeyException
    {
        RSAPublicKey rsaPublicKey = KeyValidationSupport.castKey(managementKey, RSAPublicKey.class);
        KeyValidationSupport.checkRsaKeySize(rsaPublicKey);
    }

    @Override
    public void validateDecryptionKey(Key managementKey, ContentEncryptionAlgorithm contentEncryptionAlg) throws InvalidKeyException
    {
        RSAPrivateKey rsaPrivateKey = KeyValidationSupport.castKey(managementKey, RSAPrivateKey.class);
        KeyValidationSupport.checkRsaKeySize(rsaPrivateKey);
    }

    @Override
    public boolean isAvailable()
    {
        try
        {
             return CipherUtil.getCipher(getJavaAlgorithm()) != null;
        }
        catch (JoseException e)
        {
            return false;
        }
    }

    public static class RsaOaep extends RsaKeyManagementAlgorithm implements KeyManagementAlgorithm
    {
        public RsaOaep()
        {
            super("RSA/ECB/OAEPWithSHA-1AndMGF1Padding", KeyManagementAlgorithmIdentifiers.RSA_OAEP);
        }
    }

    public static class RsaOaep256 extends RsaKeyManagementAlgorithm implements KeyManagementAlgorithm
    {
        public RsaOaep256()
        {
            super("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", KeyManagementAlgorithmIdentifiers.RSA_OAEP_256);
            setAlgorithmParameterSpec(new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
        }

        @Override
        public boolean isAvailable()
        {
            // The Sun/Oracle provider in Java 7 apparently has a defect and can’t do MGF1 with SHA-256 .
            // An exception like "java.security.InvalidKeyException: Wrapping failed ... caused by
            // javax.crypto.BadPaddingException: java.security.DigestException: Length must be at least 32 for SHA-256digests”
            // is thrown from the wrap method on the “RSA/ECB/OAEPWithSHA-256AndMGF1Padding” Cipher initialized with an
            // OAEPParameterSpec using MGF1ParameterSpec.SHA256. So actually trying it to see if it works seems like
            // the most reliable way to check for availability. Which isn’t real pretty. But hey, what can you do?
            try
            {
                JsonWebKey jwk = JsonWebKey.Factory.newJwk(
                    "{\"kty\":\"RSA\"," +
                    "\"n\":\"sXchDaQebHnPiGvyDOAT4saGEUetSyo9MKLOoWFsueri23bOdgWp4Dy1Wl" +
                    "UzewbgBHod5pcM9H95GQRV3JDXboIRROSBigeC5yjU1hGzHHyXss8UDpre" +
                    "cbAYxknTcQkhslANGRUZmdTOQ5qTRsLAt6BTYuyvVRdhS8exSZEy_c4gs_" +
                    "7svlJJQ4H9_NxsiIoLwAEk7-Q3UXERGYw_75IDrGA84-lA_-Ct4eTlXHBI" +
                    "Y2EaV7t7LjJaynVJCpkv4LKjTTAumiGUIuQhrNhZLuF_RJLqHpM2kgWFLU" +
                    "7-VTdL1VbC2tejvcI2BlMkEpk1BzBZI0KQB0GaDWFLN-aEAw3vRw\"," +
                    "\"e\":\"AQAB\"}");
                ContentEncryptionKeyDescriptor cekDesc = new ContentEncryptionKeyDescriptor(16, AesKey.ALGORITHM);
                ContentEncryptionKeys contentEncryptionKeys = manageForEncrypt(jwk.getKey(), cekDesc, null, null);
                return contentEncryptionKeys != null;
            }
            catch (JoseException e)
            {
                log.debug(getAlgorithmIdentifier() + " is not available due to " + ExceptionHelp.toStringWithCauses(e));
                return false;
            }
        }
    }

    public static class Rsa1_5 extends RsaKeyManagementAlgorithm implements KeyManagementAlgorithm
    {
        public Rsa1_5()
        {
            super("RSA/ECB/PKCS1Padding", KeyManagementAlgorithmIdentifiers.RSA1_5);
        }
    }
}
