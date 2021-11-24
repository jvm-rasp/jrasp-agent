/*
 * Copyright 2012-2013 Brian Campbell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jose4j.jwe;

import org.jose4j.base64url.Base64Url;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwa.AlgorithmFactory;
import org.jose4j.jwa.AlgorithmFactoryFactory;
import org.jose4j.jwx.CompactSerializer;
import org.jose4j.jwx.HeaderParameterNames;
import org.jose4j.jwx.Headers;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.lang.InvalidAlgorithmException;
import org.jose4j.lang.JoseException;
import org.jose4j.lang.StringUtil;
import org.jose4j.zip.CompressionAlgorithm;

import java.security.Key;

/**
 */
public class JsonWebEncryption extends JsonWebStructure
{
	public static final short COMPACT_SERIALIZATION_PARTS = 5;
	
    private Base64Url base64url = new Base64Url();
    
    private String plaintextCharEncoding = StringUtil.UTF_8;
    private byte[] plaintext;

    byte[] encryptedKey;
    byte[] iv;
    byte[] ciphertext;

    byte[] contentEncryptionKey;

    private AlgorithmConstraints contentEncryptionAlgorithmConstraints = AlgorithmConstraints.NO_CONSTRAINTS;

    public void setPlainTextCharEncoding(String plaintextCharEncoding)
    {
        this.plaintextCharEncoding = plaintextCharEncoding;
    }

    public void setPlaintext(byte[] plaintext)
    {
        this.plaintext = plaintext;
    }

    public void setPlaintext(String plaintext)
    {
        this.plaintext = StringUtil.getBytesUnchecked(plaintext, plaintextCharEncoding);
    }

    public String getPlaintextString() throws JoseException
    {
        return StringUtil.newString(getPlaintextBytes(), plaintextCharEncoding);
    }

    public byte[] getPlaintextBytes() throws JoseException
    {
        if (plaintext == null)
        {
            this.decrypt();
        }
        return plaintext;
    }

    @Override
    public String getPayload() throws JoseException
    {
        return getPlaintextString();
    }

    @Override
    public void setPayload(String payload)
    {
        setPlaintext(payload);
    }

    public void setEncryptionMethodHeaderParameter(String enc)
    {
        setHeader(HeaderParameterNames.ENCRYPTION_METHOD, enc);
    }

    public String getEncryptionMethodHeaderParameter()
    {
        return getHeader(HeaderParameterNames.ENCRYPTION_METHOD);
    }

    public void setContentEncryptionAlgorithmConstraints(AlgorithmConstraints contentEncryptionAlgorithmConstraints)
    {
        this.contentEncryptionAlgorithmConstraints = contentEncryptionAlgorithmConstraints;
    }

    public ContentEncryptionAlgorithm getContentEncryptionAlgorithm() throws InvalidAlgorithmException
    {
        String encValue = getEncryptionMethodHeaderParameter();
        if (encValue == null)
        {
            throw new InvalidAlgorithmException("Content encryption header ("+HeaderParameterNames.ENCRYPTION_METHOD+") not set.");
        }

        contentEncryptionAlgorithmConstraints.checkConstraint(encValue);
        AlgorithmFactoryFactory factoryFactory = AlgorithmFactoryFactory.getInstance();
        AlgorithmFactory<ContentEncryptionAlgorithm> factory = factoryFactory.getJweContentEncryptionAlgorithmFactory();
        return factory.getAlgorithm(encValue);
    }

    public KeyManagementAlgorithm getKeyManagementModeAlgorithm() throws InvalidAlgorithmException
    {
        String algo = getAlgorithmHeaderValue();
        if (algo == null)
        {
            throw new InvalidAlgorithmException("Encryption key management algorithm header ("+HeaderParameterNames.ALGORITHM+") not set.");
        }

        getAlgorithmConstraints().checkConstraint(algo);
        AlgorithmFactoryFactory factoryFactory = AlgorithmFactoryFactory.getInstance();
        AlgorithmFactory<KeyManagementAlgorithm> factory = factoryFactory.getJweKeyManagementAlgorithmFactory();
        return factory.getAlgorithm(algo);
    }

    protected void setCompactSerializationParts(String[] parts) throws JoseException
    {
        if (parts.length != COMPACT_SERIALIZATION_PARTS)
        {
            throw new JoseException("A JWE Compact Serialization must have exactly " + COMPACT_SERIALIZATION_PARTS + " parts separated by period ('.') characters");
        }

        setEncodedHeader(parts[0]);
        encryptedKey = base64url.base64UrlDecode(parts[1]);
        setEncodedIv(parts[2]);
        String encodedCiphertext = parts[3];
        checkNotEmptyPart(encodedCiphertext, "Encoded JWE Ciphertext");
        ciphertext = base64url.base64UrlDecode(encodedCiphertext);
        String encodedAuthenticationTag = parts[4];
        checkNotEmptyPart(encodedAuthenticationTag, "Encoded JWE Authentication Tag");
        byte[] tag = base64url.base64UrlDecode(encodedAuthenticationTag);
        setIntegrity(tag);
    }

    private void decrypt() throws JoseException
    {
        KeyManagementAlgorithm keyManagementModeAlg = getKeyManagementModeAlgorithm();
        ContentEncryptionAlgorithm contentEncryptionAlg = getContentEncryptionAlgorithm();

        ContentEncryptionKeyDescriptor contentEncryptionKeyDesc = contentEncryptionAlg.getContentEncryptionKeyDescriptor();

        if (isDoKeyValidation())
        {
            keyManagementModeAlg.validateDecryptionKey(getKey(), contentEncryptionAlg);
        }

        Key cek = keyManagementModeAlg.manageForDecrypt(getKey(), getEncryptedKey(), contentEncryptionKeyDesc, getHeaders());

        ContentEncryptionParts contentEncryptionParts = new ContentEncryptionParts(iv, ciphertext, getIntegrity());
        byte[] aad = getEncodedHeaderAsciiBytesForAdditionalAuthenticatedData();
        byte[] decrypted = contentEncryptionAlg.decrypt(contentEncryptionParts, aad, cek.getEncoded(), getHeaders());

        decrypted = decompress(getHeaders(), decrypted);

        setPlaintext(decrypted);
    }

    public byte[] getEncryptedKey()
    {
        return encryptedKey;
    }

    byte[] getEncodedHeaderAsciiBytesForAdditionalAuthenticatedData()
    {
        String encodedHeader = getEncodedHeader();
        return StringUtil.getBytesAscii(encodedHeader);
    }

    byte[] decompress(Headers headers, byte[] data) throws JoseException
    {
        String zipHeaderValue = headers.getStringHeaderValue(HeaderParameterNames.ZIP);
        if (zipHeaderValue != null)
        {
            AlgorithmFactoryFactory factoryFactory = AlgorithmFactoryFactory.getInstance();
            AlgorithmFactory<CompressionAlgorithm> zipAlgFactory = factoryFactory.getCompressionAlgorithmFactory();
            CompressionAlgorithm compressionAlgorithm = zipAlgFactory.getAlgorithm(zipHeaderValue);
            data = compressionAlgorithm.decompress(data);
        }
        return data;
    }

    byte[] compress(Headers headers, byte[] data) throws InvalidAlgorithmException
    {
        String zipHeaderValue = headers.getStringHeaderValue(HeaderParameterNames.ZIP);
        if (zipHeaderValue != null)
        {
            AlgorithmFactoryFactory factoryFactory = AlgorithmFactoryFactory.getInstance();
            AlgorithmFactory<CompressionAlgorithm> zipAlgFactory = factoryFactory.getCompressionAlgorithmFactory();
            CompressionAlgorithm compressionAlgorithm = zipAlgFactory.getAlgorithm(zipHeaderValue);
            data = compressionAlgorithm.compress(data);
        }
        return data;
    }

    public String getCompactSerialization() throws JoseException
    {
        KeyManagementAlgorithm keyManagementModeAlg = getKeyManagementModeAlgorithm();
        ContentEncryptionAlgorithm contentEncryptionAlg = getContentEncryptionAlgorithm();

        ContentEncryptionKeyDescriptor contentEncryptionKeyDesc = contentEncryptionAlg.getContentEncryptionKeyDescriptor();
        Key managementKey = getKey();
        if (isDoKeyValidation())
        {
            keyManagementModeAlg.validateEncryptionKey(getKey(), contentEncryptionAlg);
        }

        ContentEncryptionKeys contentEncryptionKeys = keyManagementModeAlg.manageForEncrypt(managementKey, contentEncryptionKeyDesc, getHeaders(), contentEncryptionKey);
        setContentEncryptionKey(contentEncryptionKeys.getContentEncryptionKey());
        encryptedKey = contentEncryptionKeys.getEncryptedKey();

        byte[] aad = getEncodedHeaderAsciiBytesForAdditionalAuthenticatedData();
        byte[] contentEncryptionKey = contentEncryptionKeys.getContentEncryptionKey();

        byte[] plaintextBytes = this.plaintext;
        if (plaintextBytes == null)
        {
            throw new NullPointerException("The plaintext payload for the JWE has not been set.");
        }

        plaintextBytes = compress(getHeaders(), plaintextBytes);

        ContentEncryptionParts contentEncryptionParts = contentEncryptionAlg.encrypt(plaintextBytes, aad, contentEncryptionKey, getHeaders(), getIv());
        setIv(contentEncryptionParts.getIv());
        ciphertext = contentEncryptionParts.getCiphertext();

        String encodedIv = base64url.base64UrlEncode(contentEncryptionParts.getIv());
        String encodedCiphertext = base64url.base64UrlEncode(contentEncryptionParts.getCiphertext());
        String encodedTag = base64url.base64UrlEncode(contentEncryptionParts.getAuthenticationTag());


        byte[] encryptedKey = contentEncryptionKeys.getEncryptedKey();
        String encodedEncryptedKey = base64url.base64UrlEncode(encryptedKey);

        return CompactSerializer.serialize(getEncodedHeader(), encodedEncryptedKey, encodedIv, encodedCiphertext, encodedTag);
    }

    public byte[] getContentEncryptionKey()
    {
        return contentEncryptionKey;
    }

    public void setContentEncryptionKey(byte[] contentEncryptionKey)
    {
        this.contentEncryptionKey = contentEncryptionKey;
    }

    public void setEncodedContentEncryptionKey(String encodedContentEncryptionKey)
    {
        setContentEncryptionKey(base64url.decode(encodedContentEncryptionKey));
    }

    public byte[] getIv()
    {
        return iv;
    }

    public void setIv(byte[] iv)
    {
        this.iv = iv;
    }

    public void setEncodedIv(String encodedIv)
    {
        setIv(base64url.base64UrlDecode(encodedIv));

    }
}
