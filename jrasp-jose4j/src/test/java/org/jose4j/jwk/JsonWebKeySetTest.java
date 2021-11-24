package org.jose4j.jwk;

import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.keys.EllipticCurves;
import org.jose4j.keys.ExampleEcKeysFromJws;
import org.jose4j.keys.ExampleRsaKeyFromJws;
import org.jose4j.lang.JoseException;

/**
 */
public class JsonWebKeySetTest extends TestCase {

	public void testParseExample() throws JoseException {
		String jwkJson = "{\"keys\":\n" + "     [\n" + "       {\"kty\":\"EC\",\n" + "        \"crv\":\"P-256\",\n"
				+ "        \"x\":\"MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4\",\n"
				+ "        \"y\":\"4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM\",\n" + "        \"use\":\"enc\",\n"
				+ "        \"kid\":\"1\"},\n" + "\n" + "       {\"kty\":\"RSA\",\n"
				+ "        \"n\": \"0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx"
				+ "   4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMs"
				+ "   tn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2"
				+ "   QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbI"
				+ "   SD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniIqb" + "   w0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw\",\n"
				+ "        \"e\":\"AQAB\",\n" + "        \"alg\":\"RS256\",\n" + "        \"kid\":\"2011-04-29\"}\n" + "     ]\n" + "   }";

		JsonWebKeySet jwkSet = new JsonWebKeySet(jwkJson);
		Collection<JsonWebKey> jwks = jwkSet.getJsonWebKeys();

		assertEquals(2, jwks.size());

		Iterator<JsonWebKey> iterator = jwks.iterator();
		assertTrue(iterator.next() instanceof EllipticCurveJsonWebKey);
		assertTrue(iterator.next() instanceof RsaJsonWebKey);

		JsonWebKey webKey1 = jwkSet.findJsonWebKey("1", null, null, null);
		assertTrue(webKey1 instanceof EllipticCurveJsonWebKey);
		assertEquals(Use.ENCRYPTION, webKey1.getUse());
		assertNotNull(webKey1.getKey());
		JsonWebKey webKey2011 = jwkSet.findJsonWebKey("2011-04-29", null, null, null);
		assertTrue(webKey2011 instanceof RsaJsonWebKey);
		assertNotNull(webKey2011.getKey());
		assertEquals(AlgorithmIdentifiers.RSA_USING_SHA256, webKey2011.getAlgorithm());

		assertEquals(Use.ENCRYPTION, jwkSet.findJsonWebKey("1", null, null, null).getUse());

		assertNull(jwkSet.findJsonWebKey("nope", null, null, null));

		String json = jwkSet.toJson();
		assertNotNull(json);
		assertTrue(json.contains("0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx"));
	}

	public void testFromRsaPublicKeyAndBack() throws JoseException {
		RsaJsonWebKey webKey = new RsaJsonWebKey(ExampleRsaKeyFromJws.PUBLIC_KEY);
		String kid = "my-key-id";
		webKey.setKeyId(kid);
		webKey.setUse(Use.SIGNATURE);
		JsonWebKeySet jwkSet = new JsonWebKeySet(Collections.<JsonWebKey> singletonList(webKey));
		String json = jwkSet.toJson();
		assertTrue(json.contains(Use.SIGNATURE));
		assertTrue(json.contains(kid));

		JsonWebKeySet parsedJwkSet = new JsonWebKeySet(json);
		Collection<JsonWebKey> webKeyKeyObjects = parsedJwkSet.getJsonWebKeys();
		assertEquals(1, webKeyKeyObjects.size());
		JsonWebKey jwk = parsedJwkSet.findJsonWebKey(kid, null, null, null);
		assertEquals(RsaJsonWebKey.KEY_TYPE, jwk.getKeyType());
		assertEquals(kid, jwk.getKeyId());
		assertEquals(Use.SIGNATURE, jwk.getUse());

		RsaJsonWebKey rsaJsonWebKey = (RsaJsonWebKey) jwk;
		assertEquals(ExampleRsaKeyFromJws.PUBLIC_KEY.getModulus(), rsaJsonWebKey.getRsaPublicKey().getModulus());
		assertEquals(ExampleRsaKeyFromJws.PUBLIC_KEY.getPublicExponent(), rsaJsonWebKey.getRsaPublicKey().getPublicExponent());
	}

	public void testFromEcPublicKeyAndBack() throws JoseException {

		for (ECPublicKey publicKey : new ECPublicKey[] { ExampleEcKeysFromJws.PUBLIC_256, ExampleEcKeysFromJws.PUBLIC_521 }) {
			EllipticCurveJsonWebKey webKey = new EllipticCurveJsonWebKey(publicKey);
			String kid = "kkiidd";
			webKey.setKeyId(kid);
			webKey.setUse(Use.ENCRYPTION);
			JsonWebKeySet jwkSet = new JsonWebKeySet(Collections.<JsonWebKey> singletonList(webKey));
			String json = jwkSet.toJson();

			assertTrue(json.contains(Use.ENCRYPTION));
			assertTrue(json.contains(kid));

			JsonWebKeySet parsedJwkSet = new JsonWebKeySet(json);
			Collection<JsonWebKey> webKeyKeyObjects = parsedJwkSet.getJsonWebKeys();
			assertEquals(1, webKeyKeyObjects.size());
			JsonWebKey jwk = parsedJwkSet.findJsonWebKey(kid, null, null, null);
			assertEquals(EllipticCurveJsonWebKey.KEY_TYPE, jwk.getKeyType());
			assertEquals(kid, jwk.getKeyId());
			assertEquals(Use.ENCRYPTION, jwk.getUse());

			EllipticCurveJsonWebKey ecJsonWebKey = (EllipticCurveJsonWebKey) jwk;
			assertEquals(publicKey.getW().getAffineX(), ecJsonWebKey.getECPublicKey().getW().getAffineX());
			assertEquals(publicKey.getW().getAffineY(), ecJsonWebKey.getECPublicKey().getW().getAffineY());
			assertEquals(publicKey.getParams().getCofactor(), ecJsonWebKey.getECPublicKey().getParams().getCofactor());
			assertEquals(publicKey.getParams().getCurve(), ecJsonWebKey.getECPublicKey().getParams().getCurve());
			assertEquals(publicKey.getParams().getGenerator(), ecJsonWebKey.getECPublicKey().getParams().getGenerator());
			assertEquals(publicKey.getParams().getOrder(), ecJsonWebKey.getECPublicKey().getParams().getOrder());
		}
	}

	public void testCreateFromListOfPubJwks() throws JoseException {
		List<PublicJsonWebKey> ecjwks = new ArrayList<PublicJsonWebKey>();
		ecjwks.add(EcJwkGenerator.generateJwk(EllipticCurves.P256));
		ecjwks.add(EcJwkGenerator.generateJwk(EllipticCurves.P256));
		JsonWebKeySet jsonWebKeySet = new JsonWebKeySet(ecjwks);
		assertEquals(2, jsonWebKeySet.getJsonWebKeys().size());
	}

	public void testOctAndDefaultToJson() throws JoseException {
		JsonWebKeySet jwks = new JsonWebKeySet(OctJwkGenerator.generateJwk(128), OctJwkGenerator.generateJwk(128));
		String json = jwks.toJson();
		assertTrue(json.contains("\"k\""));

		JsonWebKeySet newJwks = new JsonWebKeySet(json);
		assertEquals(jwks.getJsonWebKeys().size(), newJwks.getJsonWebKeys().size());
	}

}
