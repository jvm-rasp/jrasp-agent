package org.jose4j.jwe;

import org.jose4j.jwa.AlgorithmInfo;
import org.jose4j.jwk.EllipticCurveJsonWebKey;
import org.jose4j.jwx.HeaderParameterNames;
import org.jose4j.jwx.Headers;
import org.jose4j.keys.AesKey;
import org.jose4j.keys.KeyPersuasion;
import org.jose4j.lang.ByteUtil;
import org.jose4j.lang.InvalidKeyException;
import org.jose4j.lang.JoseException;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

/**
 */
public class EcdhKeyAgreementWithAesKeyWrapAlgorithm extends AlgorithmInfo implements KeyManagementAlgorithm
{
    private AesKeyWrapManagementAlgorithm keyWrap;

    private ContentEncryptionKeyDescriptor keyWrapKeyDescriptor;

    private EcdhKeyAgreementAlgorithm ecdh;

    public EcdhKeyAgreementWithAesKeyWrapAlgorithm(String alg, AesKeyWrapManagementAlgorithm keyWrapAlgorithm)
    {
        setAlgorithmIdentifier(alg);
        setJavaAlgorithm("N/A");
        setKeyType(EllipticCurveJsonWebKey.KEY_TYPE);
        setKeyPersuasion(KeyPersuasion.ASYMMETRIC);
        this.keyWrap = keyWrapAlgorithm;
        this.ecdh = new EcdhKeyAgreementAlgorithm(HeaderParameterNames.ALGORITHM);
        keyWrapKeyDescriptor = new ContentEncryptionKeyDescriptor(keyWrapAlgorithm.getKeyByteLength(), AesKey.ALGORITHM);
    }

    public ContentEncryptionKeys manageForEncrypt(Key managementKey, ContentEncryptionKeyDescriptor cekDesc, Headers headers, byte[] cekOverride)
            throws JoseException
    {
        ContentEncryptionKeys agreedKeys = ecdh.manageForEncrypt(managementKey, keyWrapKeyDescriptor, headers, (byte[])null);
        String contentEncryptionKeyAlgorithm = keyWrapKeyDescriptor.getContentEncryptionKeyAlgorithm();
        Key agreedKey = new SecretKeySpec(agreedKeys.getContentEncryptionKey(), contentEncryptionKeyAlgorithm);
        return keyWrap.manageForEncrypt(agreedKey, cekDesc, headers, cekOverride);
    }

    public Key manageForDecrypt(Key managementKey, byte[] encryptedKey, ContentEncryptionKeyDescriptor cekDesc, Headers headers)
            throws JoseException
    {
        Key agreedKey = ecdh.manageForDecrypt(managementKey, ByteUtil.EMPTY_BYTES, keyWrapKeyDescriptor, headers);
        return keyWrap.manageForDecrypt(agreedKey, encryptedKey, cekDesc, headers);
    }

    @Override
    public void validateEncryptionKey(Key managementKey, ContentEncryptionAlgorithm contentEncryptionAlg) throws InvalidKeyException
    {
        ecdh.validateEncryptionKey(managementKey, contentEncryptionAlg);
    }

    @Override
    public void validateDecryptionKey(Key managementKey, ContentEncryptionAlgorithm contentEncryptionAlg) throws InvalidKeyException
    {
        ecdh.validateDecryptionKey(managementKey, contentEncryptionAlg);
    }

    @Override
    public boolean isAvailable()
    {
        return ecdh.isAvailable() && keyWrap.isAvailable();
    }

    public static class EcdhKeyAgreementWithAes128KeyWrapAlgorithm extends EcdhKeyAgreementWithAesKeyWrapAlgorithm implements KeyManagementAlgorithm
    {
        public EcdhKeyAgreementWithAes128KeyWrapAlgorithm()
        {
            super(KeyManagementAlgorithmIdentifiers.ECDH_ES_A128KW, new AesKeyWrapManagementAlgorithm.Aes128());
        }
    }

    public static class EcdhKeyAgreementWithAes192KeyWrapAlgorithm extends EcdhKeyAgreementWithAesKeyWrapAlgorithm implements KeyManagementAlgorithm
    {
        public EcdhKeyAgreementWithAes192KeyWrapAlgorithm()
        {
            super(KeyManagementAlgorithmIdentifiers.ECDH_ES_A192KW, new AesKeyWrapManagementAlgorithm.Aes192());
        }
    }

    public static class EcdhKeyAgreementWithAes256KeyWrapAlgorithm extends EcdhKeyAgreementWithAesKeyWrapAlgorithm implements KeyManagementAlgorithm
    {
        public EcdhKeyAgreementWithAes256KeyWrapAlgorithm()
        {
            super(KeyManagementAlgorithmIdentifiers.ECDH_ES_A256KW, new AesKeyWrapManagementAlgorithm.Aes256());
        }
    }
}
