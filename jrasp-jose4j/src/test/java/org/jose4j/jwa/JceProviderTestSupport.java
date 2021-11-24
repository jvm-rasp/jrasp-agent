package org.jose4j.jwa;

import java.security.Security;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.jwe.ContentEncryptionAlgorithm;
import org.jose4j.jwe.KeyManagementAlgorithm;
import org.jose4j.jws.JsonWebSignatureAlgorithm;

/**
 *
 */
public class JceProviderTestSupport {

	private boolean doReinitialize = true;

	private Set<String> signatureAlgs = Collections.emptySet();

	private Set<String> keyManagementAlgs = Collections.emptySet();

	private Set<String> encryptionAlgs = Collections.emptySet();

	private void reinitialize() {
		AlgorithmFactoryFactory.getInstance().reinitialize();
	}

	public void runWithBouncyCastleProviderIfNeeded(RunnableTest test) throws Exception {
		BouncyCastleProvider bouncyCastleProvider = new BouncyCastleProvider();
		boolean needBouncyCastle = false;

		AlgorithmFactoryFactory aff = AlgorithmFactoryFactory.getInstance();

		AlgorithmFactory<JsonWebSignatureAlgorithm> jwsAlgorithmFactory = aff.getJwsAlgorithmFactory();
		if (!jwsAlgorithmFactory.getSupportedAlgorithms().containsAll(signatureAlgs)) {
			needBouncyCastle = true;
		}

		AlgorithmFactory<KeyManagementAlgorithm> jweKeyMgmtAlgorithmFactory = aff.getJweKeyManagementAlgorithmFactory();
		if (!jweKeyMgmtAlgorithmFactory.getSupportedAlgorithms().containsAll(keyManagementAlgs)) {
			needBouncyCastle = true;
		}

		AlgorithmFactory<ContentEncryptionAlgorithm> jweEncAlgFactory = aff.getJweContentEncryptionAlgorithmFactory();
		if (!jweEncAlgFactory.getSupportedAlgorithms().containsAll(encryptionAlgs)) {
			needBouncyCastle = true;
		}

		boolean removeBouncyCastle = true;
		try {
			if (needBouncyCastle) {
				int position = Security.insertProviderAt(bouncyCastleProvider, 1);
				removeBouncyCastle = (position != -1);
				if (doReinitialize) {
					reinitialize();
				}
			}

			test.runTest();
		} finally {
			if (needBouncyCastle) {
				if (removeBouncyCastle) {
					Security.removeProvider(bouncyCastleProvider.getName());
				}

				if (doReinitialize) {
					reinitialize();
				}
			}
		}
	}

	public void setSignatureAlgsNeeded(String... algs) {
		signatureAlgs = new HashSet<String>(Arrays.asList(algs));
	}

	public void setKeyManagementAlgsNeeded(String... algs) {
		keyManagementAlgs = new HashSet<String>(Arrays.asList(algs));
	}

	public void setEncryptionAlgsNeeded(String... algs) {
		encryptionAlgs = new HashSet<String>(Arrays.asList(algs));
	}

	public void setDoReinitialize(boolean doReinitialize) {
		this.doReinitialize = doReinitialize;
	}

	public static interface RunnableTest {

		public abstract void runTest() throws Exception;
	}
}
