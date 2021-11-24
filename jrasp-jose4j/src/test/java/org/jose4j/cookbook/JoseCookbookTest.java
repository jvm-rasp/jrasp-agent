/*
 * Copyright 2012-2014 Brian Campbell
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

package org.jose4j.cookbook;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.jose4j.base64url.Base64Url;
import org.jose4j.jwa.JceProviderTestSupport;
import org.jose4j.jwa.JceProviderTestSupport.RunnableTest;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwx.CompactSerializer;
import org.jose4j.jwx.HeaderParameterNames;
import org.jose4j.jwx.Headers;
import org.jose4j.keys.PbkdfKey;
import org.jose4j.lang.JoseException;
import org.junit.Test;

/**
 * Tests of the examples from the JOSE Cookbook. Started with
 * http://tools.ietf.org/html/draft-ietf-jose-cookbook-01 and then worked with
 * incremental updates from Matt at https://github.com/linuxwolf/jose-more and
 * eventually resulted in http://tools.ietf.org/html/draft-ietf-jose-cookbook-02
 * that incorporates fixes to issues found while doing all this. Way at the end
 * on page 94 there's even acknowledgement of it
 * http://tools.ietf.org/html/draft-ietf-jose-cookbook-02#appendix-A
 * 
 * 3.1. RSA v1.5 Signature 3.2. RSA-PSS Signature (via the the Bouncy Castle
 * provider) 3.3. ECDSA Signature 3.4. HMAC-SHA2 Integrity Protection 3.5.
 * Detached Signature
 * 
 * 4.1. Key Encryption using RSA v1.5 and AES-HMAC-SHA2 4.2. Key Encryption
 * using RSA-OAEP with A256GCM 4.3. Key Wrap using PBES2-AES-KeyWrap with
 * AES-CBC-HMAC-SHA2 4.4. Key Agreement with Key Wrapping using ECDH-ES and
 * AES-KeyWrap with AES-GCM 4.5. Key Agreement using ECDH-ES with
 * AES-CBC-HMAC-SHA2 4.6. Direct Encryption using AES-GCM 4.7. Key Wrap using
 * AES-GCM KeyWrap with AES-CBC-HMAC-SHA2 4.8. Key Wrap using AES-KeyWrap with
 * AES-GCM 4.9. Compressed Content
 * 
 * 5. Nesting Signatures and Encryption
 * 
 */
public class JoseCookbookTest {

	// http://tools.ietf.org/html/draft-ietf-jose-cookbook-01#section-3
	String encodedJwsPayload = "SXTigJlzIGEgZGFuZ2Vyb3VzIGJ1c2luZXNzLCBGcm9kbywgZ29pbmcgb3V0IH"
			+ "lvdXIgZG9vci4gWW91IHN0ZXAgb250byB0aGUgcm9hZCwgYW5kIGlmIHlvdSBk"
			+ "b24ndCBrZWVwIHlvdXIgZmVldCwgdGhlcmXigJlzIG5vIGtub3dpbmcgd2hlcm" + "UgeW91IG1pZ2h0IGJlIHN3ZXB0IG9mZiB0by4";

	String jwsPayload = Base64Url.decodeToUtf8String(encodedJwsPayload);

	// http://tools.ietf.org/html/draft-ietf-jose-cookbook-01#section-4
	String jwePlaintext = "You can trust us to stick with you through thick and " + "thin–to the bitter end. And you can trust us to "
			+ "keep any secret of yours–closer than you keep it " + "yourself. But you cannot trust us to let you face trouble "
			+ "alone, and go off without a word. We are your friends, Frodo.";

	String figure3RsaJwkJsonString = "{" + "  \"kty\": \"RSA\"," + "  \"kid\": \"bilbo.baggins@hobbiton.example\",\n"
			+ "  \"use\": \"sig\",\n" + "  \"n\": \"n4EPtAOCc9AlkeQHPzHStgAbgs7bTZLwUBZdR8_KuKPEHLd4rHVTeT\n"
			+ "      -O-XV2jRojdNhxJWTDvNd7nqQ0VEiZQHz_AJmSCpMaJMRBSFKrKb2wqV\n"
			+ "      wGU_NsYOYL-QtiWN2lbzcEe6XC0dApr5ydQLrHqkHHig3RBordaZ6Aj-\n"
			+ "      oBHqFEHYpPe7Tpe-OfVfHd1E6cS6M1FZcD1NNLYD5lFHpPI9bTwJlsde\n"
			+ "      3uhGqC0ZCuEHg8lhzwOHrtIQbS0FVbb9k3-tVTU4fg_3L_vniUFAKwuC\n"
			+ "      LqKnS2BYwdq_mzSnbLY7h_qixoR7jig3__kRhuaxwUkRz5iaiQkqgc5g\n" + "      HdrNP5zw\",\n" + "  \"e\": \"AQAB\",\n"
			+ "  \"d\": \"bWUC9B-EFRIo8kpGfh0ZuyGPvMNKvYWNtB_ikiH9k20eT-O1q_I78e\n"
			+ "      iZkpXxXQ0UTEs2LsNRS-8uJbvQ-A1irkwMSMkK1J3XTGgdrhCku9gRld\n"
			+ "      Y7sNA_AKZGh-Q661_42rINLRCe8W-nZ34ui_qOfkLnK9QWDDqpaIsA-b\n"
			+ "      MwWWSDFu2MUBYwkHTMEzLYGqOe04noqeq1hExBTHBOBdkMXiuFhUq1BU\n"
			+ "      6l-DqEiWxqg82sXt2h-LMnT3046AOYJoRioz75tSUQfGCshWTBnP5uDj\n"
			+ "      d18kKhyv07lhfSJdrPdM5Plyl21hsFf4L_mHCuoFau7gdsPfHPxxjVOc\n" + "      OpBrQzwQ\",\n"
			+ "  \"p\": \"3Slxg_DwTXJcb6095RoXygQCAZ5RnAvZlno1yhHtnUex_fp7AZ_9nR\n"
			+ "      aO7HX_-SFfGQeutao2TDjDAWU4Vupk8rw9JR0AzZ0N2fvuIAmr_WCsmG\n"
			+ "      peNqQnev1T7IyEsnh8UMt-n5CafhkikzhEsrmndH6LxOrvRJlsPp6Zv8\n" + "      bUq0k\",\n"
			+ "  \"q\": \"uKE2dh-cTf6ERF4k4e_jy78GfPYUIaUyoSSJuBzp3Cubk3OCqs6grT\n"
			+ "      8bR_cu0Dm1MZwWmtdqDyI95HrUeq3MP15vMMON8lHTeZu2lmKvwqW7an\n"
			+ "      V5UzhM1iZ7z4yMkuUwFWoBvyY898EXvRD-hdqRxHlSqAZ192zB3pVFJ0\n" + "      s7pFc\",\n"
			+ "  \"dp\": \"B8PVvXkvJrj2L-GYQ7v3y9r6Kw5g9SahXBwsWUzp19TVlgI-YV85q\n"
			+ "      1NIb1rxQtD-IsXXR3-TanevuRPRt5OBOdiMGQp8pbt26gljYfKU_E9xn\n"
			+ "      -RULHz0-ed9E9gXLKD4VGngpz-PfQ_q29pk5xWHoJp009Qf1HvChixRX\n" + "      59ehik\",\n"
			+ "  \"dq\": \"CLDmDGduhylc9o7r84rEUVn7pzQ6PF83Y-iBZx5NT-TpnOZKF1pEr\n"
			+ "      AMVeKzFEl41DlHHqqBLSM0W1sOFbwTxYWZDm6sI6og5iTbwQGIC3gnJK\n"
			+ "      bi_7k_vJgGHwHxgPaX2PnvP-zyEkDERuf-ry4c_Z11Cq9AqC2yeL6kdK\n" + "      T1cYF8\",\n"
			+ "  \"qi\": \"3PiqvXQN0zwMeE-sBvZgi289XP9XCQF3VWqPzMKnIgQp7_Tugo6-N\n"
			+ "      ZBKCQsMf3HaEGBjTVJs_jcK8-TRXvaKe-7ZMaQj8VfBdYkssbu0NKDDh\n"
			+ "      jJ-GtiseaDVWt7dcH0cfwxgFUHpQh7FoCrjFJ6h6ZEpMF6xmujs4qMpP\n" + "      z8aaI4\"\n" + "}";

	String figure62RsaJwkJsonString = "{\n" + "  \"kty\": \"RSA\",\n" + "  \"kid\": \"samwise.gamgee@hobbiton.example\",\n"
			+ "  \"use\": \"enc\",\n" + "  \"n\": \"wbdxI55VaanZXPY29Lg5hdmv2XhvqAhoxUkanfzf2-5zVUxa6prHRr\n"
			+ "      I4pP1AhoqJRlZfYtWWd5mmHRG2pAHIlh0ySJ9wi0BioZBl1XP2e-C-Fy\n"
			+ "      XJGcTy0HdKQWlrfhTm42EW7Vv04r4gfao6uxjLGwfpGrZLarohiWCPnk\n"
			+ "      Nrg71S2CuNZSQBIPGjXfkmIy2tl_VWgGnL22GplyXj5YlBLdxXp3XeSt\n"
			+ "      sqo571utNfoUTU8E4qdzJ3U1DItoVkPGsMwlmmnJiwA7sXRItBCivR4M\n"
			+ "      5qnZtdw-7v4WuR4779ubDuJ5nalMv2S66-RPcnFAzWSKxtBDnFJJDGIU\n"
			+ "      e7Tzizjg1nms0Xq_yPub_UOlWn0ec85FCft1hACpWG8schrOBeNqHBOD\n"
			+ "      FskYpUc2LC5JA2TaPF2dA67dg1TTsC_FupfQ2kNGcE1LgprxKHcVWYQb\n"
			+ "      86B-HozjHZcqtauBzFNV5tbTuB-TpkcvJfNcFLlH3b8mb-H_ox35FjqB\n"
			+ "      SAjLKyoeqfKTpVjvXhd09knwgJf6VKq6UC418_TOljMVfFTWXUxlnfhO\n"
			+ "      OnzW6HSSzD1c9WrCuVzsUMv54szidQ9wf1cYWf3g5qFDxDQKis99gcDa\n"
			+ "      iCAwM3yEBIzuNeeCa5dartHDb1xEB_HcHSeYbghbMjGfasvKn0aZRsnT\n" + "      yC0xhWBlsolZE\",\n" + "  \"e\": \"AQAB\",\n"
			+ "  \"alg\": \"RSA-OAEP\",\n" + "  \"d\": \"n7fzJc3_WG59VEOBTkayzuSMM780OJQuZjN_KbH8lOZG25ZoA7T4Bx\n"
			+ "      cc0xQn5oZE5uSCIwg91oCt0JvxPcpmqzaJZg1nirjcWZ-oBtVk7gCAWq\n"
			+ "      -B3qhfF3izlbkosrzjHajIcY33HBhsy4_WerrXg4MDNE4HYojy68TcxT\n"
			+ "      2LYQRxUOCf5TtJXvM8olexlSGtVnQnDRutxEUCwiewfmmrfveEogLx9E\n"
			+ "      A-KMgAjTiISXxqIXQhWUQX1G7v_mV_Hr2YuImYcNcHkRvp9E7ook0876\n"
			+ "      DhkO8v4UOZLwA1OlUX98mkoqwc58A_Y2lBYbVx1_s5lpPsEqbbH-nqIj\n"
			+ "      h1fL0gdNfihLxnclWtW7pCztLnImZAyeCWAG7ZIfv-Rn9fLIv9jZ6r7r\n"
			+ "      -MSH9sqbuziHN2grGjD_jfRluMHa0l84fFKl6bcqN1JWxPVhzNZo01yD\n"
			+ "      F-1LiQnqUYSepPf6X3a2SOdkqBRiquE6EvLuSYIDpJq3jDIsgoL8Mo1L\n"
			+ "      oomgiJxUwL_GWEOGu28gplyzm-9Q0U0nyhEf1uhSR8aJAQWAiFImWH5W\n"
			+ "      _IQT9I7-yrindr_2fWQ_i1UgMsGzA7aOGzZfPljRy6z-tY_KuBG00-28\n"
			+ "      S_aWvjyUc-Alp8AUyKjBZ-7CWH32fGWK48j1t-zomrwjL_mnhsPbGs0c\n" + "      9WsWgRzI-K8gE\",\n"
			+ "  \"p\": \"7_2v3OQZzlPFcHyYfLABQ3XP85Es4hCdwCkbDeltaUXgVy9l9etKgh\n"
			+ "      vM4hRkOvbb01kYVuLFmxIkCDtpi-zLCYAdXKrAK3PtSbtzld_XZ9nlsY\n"
			+ "      a_QZWpXB_IrtFjVfdKUdMz94pHUhFGFj7nr6NNxfpiHSHWFE1zD_AC3m\n"
			+ "      Y46J961Y2LRnreVwAGNw53p07Db8yD_92pDa97vqcZOdgtybH9q6uma-\n"
			+ "      RFNhO1AoiJhYZj69hjmMRXx-x56HO9cnXNbmzNSCFCKnQmn4GQLmRj9s\n"
			+ "      fbZRqL94bbtE4_e0Zrpo8RNo8vxRLqQNwIy85fc6BRgBJomt8QdQvIgP\n" + "      gWCv5HoQ\",\n"
			+ "  \"q\": \"zqOHk1P6WN_rHuM7ZF1cXH0x6RuOHq67WuHiSknqQeefGBA9PWs6Zy\n"
			+ "      KQCO-O6mKXtcgE8_Q_hA2kMRcKOcvHil1hqMCNSXlflM7WPRPZu2qCDc\n"
			+ "      qssd_uMbP-DqYthH_EzwL9KnYoH7JQFxxmcv5An8oXUtTwk4knKjkIYG\n"
			+ "      RuUwfQTus0w1NfjFAyxOOiAQ37ussIcE6C6ZSsM3n41UlbJ7TCqewzVJ\n"
			+ "      aPJN5cxjySPZPD3Vp01a9YgAD6a3IIaKJdIxJS1ImnfPevSJQBE79-EX\n"
			+ "      e2kSwVgOzvt-gsmM29QQ8veHy4uAqca5dZzMs7hkkHtw1z0jHV90epQJ\n" + "      JlXXnH8Q\",\n"
			+ "  \"dp\": \"19oDkBh1AXelMIxQFm2zZTqUhAzCIr4xNIGEPNoDt1jK83_FJA-xn\n"
			+ "      x5kA7-1erdHdms_Ef67HsONNv5A60JaR7w8LHnDiBGnjdaUmmuO8XAxQ\n"
			+ "      J_ia5mxjxNjS6E2yD44USo2JmHvzeeNczq25elqbTPLhUpGo1IZuG72F\n"
			+ "      ZQ5gTjXoTXC2-xtCDEUZfaUNh4IeAipfLugbpe0JAFlFfrTDAMUFpC3i\n"
			+ "      XjxqzbEanflwPvj6V9iDSgjj8SozSM0dLtxvu0LIeIQAeEgT_yXcrKGm\n"
			+ "      pKdSO08kLBx8VUjkbv_3Pn20Gyu2YEuwpFlM_H1NikuxJNKFGmnAq9Lc\n" + "      nwwT0jvoQ\",\n"
			+ "  \"dq\": \"S6p59KrlmzGzaQYQM3o0XfHCGvfqHLYjCO557HYQf72O9kLMCfd_1\n"
			+ "      VBEqeD-1jjwELKDjck8kOBl5UvohK1oDfSP1DleAy-cnmL29DqWmhgwM\n"
			+ "      1ip0CCNmkmsmDSlqkUXDi6sAaZuntyukyflI-qSQ3C_BafPyFaKrt1fg\n"
			+ "      dyEwYa08pESKwwWisy7KnmoUvaJ3SaHmohFS78TJ25cfc10wZ9hQNOrI\n"
			+ "      ChZlkiOdFCtxDqdmCqNacnhgE3bZQjGp3n83ODSz9zwJcSUvODlXBPc2\n"
			+ "      AycH6Ci5yjbxt4Ppox_5pjm6xnQkiPgj01GpsUssMmBN7iHVsrE7N2iz\n" + "      nBNCeOUIQ\",\n"
			+ "  \"qi\": \"FZhClBMywVVjnuUud-05qd5CYU0dK79akAgy9oX6RX6I3IIIPckCc\n"
			+ "      iRrokxglZn-omAY5CnCe4KdrnjFOT5YUZE7G_Pg44XgCXaarLQf4hl80\n"
			+ "      oPEf6-jJ5Iy6wPRx7G2e8qLxnh9cOdf-kRqgOS3F48Ucvw3ma5V6KGMw\n"
			+ "      QqWFeV31XtZ8l5cVI-I3NzBS7qltpUVgz2Ju021eyc7IlqgzR98qKONl\n"
			+ "      27DuEES0aK0WE97jnsyO27Yp88Wa2RiBrEocM89QZI1seJiGDizHRUP4\n"
			+ "      UZxw9zsXww46wy0P6f9grnYp7t8LkyDDk8eoI4KX6SNMNVcyVS9IWjlq\n" + "      8EzqZEKIA\"\n" + "}";

	@Test
	public void rsa_v1_5Signature_3_1() throws JoseException {
		String jwsCompactSerialization = "eyJhbGciOiJSUzI1NiIsImtpZCI6ImJpbGJvLmJhZ2dpbnNAaG9iYml0b24uZX" + "hhbXBsZSJ9" + "."
				+ "SXTigJlzIGEgZGFuZ2Vyb3VzIGJ1c2luZXNzLCBGcm9kbywgZ29pbmcgb3V0IH"
				+ "lvdXIgZG9vci4gWW91IHN0ZXAgb250byB0aGUgcm9hZCwgYW5kIGlmIHlvdSBk"
				+ "b24ndCBrZWVwIHlvdXIgZmVldCwgdGhlcmXigJlzIG5vIGtub3dpbmcgd2hlcm" + "UgeW91IG1pZ2h0IGJlIHN3ZXB0IG9mZiB0by4" + "."
				+ "MRjdkly7_-oTPTS3AXP41iQIGKa80A0ZmTuV5MEaHoxnW2e5CZ5NlKtainoFmK"
				+ "ZopdHM1O2U4mwzJdQx996ivp83xuglII7PNDi84wnB-BDkoBwA78185hX-Es4J"
				+ "IwmDLJK3lfWRa-XtL0RnltuYv746iYTh_qHRD68BNt1uSNCrUCTJDt5aAE6x8w"
				+ "W1Kt9eRo4QPocSadnHXFxnt8Is9UzpERV0ePPQdLuW3IS_de3xyIrDaLGdjluP"
				+ "xUAhb6L2aXic1U12podGU0KLUQSE_oI-ZnmKJ3F4uOZDnd6QZWJushZ41Axf_f" + "cIe8u9ipH84ogoree7vjbU5y18kDquDg";

		String alg = AlgorithmIdentifiers.RSA_USING_SHA256;

		// verify consuming the JWS
		JsonWebSignature jws = new JsonWebSignature();
		jws.setCompactSerialization(jwsCompactSerialization);
		JsonWebKey jwk = JsonWebKey.Factory.newJwk(figure3RsaJwkJsonString);
		jws.setKey(jwk.getKey());
		assertThat(jws.verifySignature(), is(true));
		assertThat(jws.getPayload(), equalTo(jwsPayload));
		assertThat(jws.getKeyIdHeaderValue(), equalTo(jwk.getKeyId()));
		assertThat(alg, equalTo(jws.getAlgorithmHeaderValue()));

		// verify reproducing it (it's just luck that using the setters for the
		// headers results in the exact same
		// JSON representation of the header)
		jws = new JsonWebSignature();
		jws.setPayload(jwsPayload);
		jws.setAlgorithmHeaderValue(alg);
		jws.setKeyIdHeaderValue(jwk.getKeyId());
		PublicJsonWebKey rsaJwk = (PublicJsonWebKey) jwk;
		jws.setKey(rsaJwk.getPrivateKey());
		String compactSerialization = jws.getCompactSerialization();
		assertThat(jwsCompactSerialization, equalTo(compactSerialization));
	}

	@Test
	public void rsaPssSignature_3_2() throws Exception {
		final String rsaPssUsingSha384 = AlgorithmIdentifiers.RSA_PSS_USING_SHA384;
		JceProviderTestSupport jceProviderTestSupport = new JceProviderTestSupport();
		jceProviderTestSupport.setSignatureAlgsNeeded(rsaPssUsingSha384);
		jceProviderTestSupport.runWithBouncyCastleProviderIfNeeded(new RunnableTest() {

			@Override
			public void runTest() throws JoseException {
				PublicJsonWebKey jwk = PublicJsonWebKey.Factory.newPublicJwk(figure3RsaJwkJsonString);

				String cs = "eyJhbGciOiJQUzM4NCIsImtpZCI6ImJpbGJvLmJhZ2dpbnNAaG9iYml0b24uZX" + "hhbXBsZSJ9" + "."
						+ "SXTigJlzIGEgZGFuZ2Vyb3VzIGJ1c2luZXNzLCBGcm9kbywgZ29pbmcgb3V0IH"
						+ "lvdXIgZG9vci4gWW91IHN0ZXAgb250byB0aGUgcm9hZCwgYW5kIGlmIHlvdSBk"
						+ "b24ndCBrZWVwIHlvdXIgZmVldCwgdGhlcmXigJlzIG5vIGtub3dpbmcgd2hlcm" + "UgeW91IG1pZ2h0IGJlIHN3ZXB0IG9mZiB0by4" + "."
						+ "cu22eBqkYDKgIlTpzDXGvaFfz6WGoz7fUDcfT0kkOy42miAh2qyBzk1xEsnk2I"
						+ "pN6-tPid6VrklHkqsGqDqHCdP6O8TTB5dDDItllVo6_1OLPpcbUrhiUSMxbbXU"
						+ "vdvWXzg-UD8biiReQFlfz28zGWVsdiNAUf8ZnyPEgVFn442ZdNqiVJRmBqrYRX"
						+ "e8P_ijQ7p8Vdz0TTrxUeT3lm8d9shnr2lfJT8ImUjvAA2Xez2Mlp8cBE5awDzT"
						+ "0qI0n6uiP1aCN_2_jLAeQTlqRHtfa64QQSUmFAAjVKPbByi7xho0uTOcbH510a" + "6GYmJUAfmWjwZ6oD4ifKo8DYM-X72Eaw";

				JsonWebSignature jws = new JsonWebSignature();
				jws.setCompactSerialization(cs);
				jws.setKey(jwk.getPublicKey());
				assertThat(jws.verifySignature(), is(true));
				assertThat(jws.getPayload(), equalTo(jwsPayload));
				assertThat(jws.getKeyIdHeaderValue(), equalTo(jwk.getKeyId()));
				assertThat(rsaPssUsingSha384, equalTo(jws.getAlgorithmHeaderValue()));

				// can't easily verify reproducing RSA-PSS because "it is
				// probabilistic rather than deterministic,
				// incorporating a randomly generated salt value" - from
				// http://tools.ietf.org/html/rfc3447#section-8.1
			}
		});

	}

	@Test
	public void ecdsaSignature_3_3() throws JoseException {
		String jwkJson = "{\n" + "  \"kty\": \"EC\",\n" + "  \"kid\": \"bilbo.baggins@hobbiton.example\",\n" + "  \"use\": \"sig\",\n"
				+ "  \"crv\": \"P-521\",\n" + "  \"x\": \"AHKZLLOsCOzz5cY97ewNUajB957y-C-U88c3v13nmGZx6sYl_oJXu9\n"
				+ "      A5RkTKqjqvjyekWF-7ytDyRXYgCF5cj0Kt\",\n" + "  \"y\": \"AdymlHvOiLxXkEhayXQnNCvDX4h9htZaCJN34kfmC6pV5OhQHiraVy\n"
				+ "      SsUdaQkAgDPrwQrJmbnX9cwlGfP-HqHZR1\",\n" + "  \"d\": \"AAhRON2r9cqXX1hg-RoI6R1tX5p2rUAYdmpHZoC1XNM56KtscrX6zb\n"
				+ "      KipQrCW9CGZH3T4ubpnoTKLDYJ_fF3_rJt\"\n" + "}";

		String jwsCompactSerialization = "eyJhbGciOiJFUzUxMiIsImtpZCI6ImJpbGJvLmJhZ2dpbnNAaG9iYml0b24uZX" + "hhbXBsZSJ9" + "."
				+ "SXTigJlzIGEgZGFuZ2Vyb3VzIGJ1c2luZXNzLCBGcm9kbywgZ29pbmcgb3V0IH"
				+ "lvdXIgZG9vci4gWW91IHN0ZXAgb250byB0aGUgcm9hZCwgYW5kIGlmIHlvdSBk"
				+ "b24ndCBrZWVwIHlvdXIgZmVldCwgdGhlcmXigJlzIG5vIGtub3dpbmcgd2hlcm" + "UgeW91IG1pZ2h0IGJlIHN3ZXB0IG9mZiB0by4" + "."
				+ "AE_R_YZCChjn4791jSQCrdPZCNYqHXCTZH0-JZGYNlaAjP2kqaluUIIUnC9qvb"
				+ "u9Plon7KRTzoNEuT4Va2cmL1eJAQy3mtPBu_u_sDDyYjnAMDxXPn7XrT0lw-kv" + "AD890jl8e2puQens_IEKBpHABlsbEPX6sFY8OcGDqoRuBomu9xQ2";

		String alg = AlgorithmIdentifiers.ECDSA_USING_P521_CURVE_AND_SHA512;

		// verify consuming the JWS
		JsonWebSignature jws = new JsonWebSignature();
		jws.setCompactSerialization(jwsCompactSerialization);
		JsonWebKey jwk = JsonWebKey.Factory.newJwk(jwkJson);

		jws.setKey(jwk.getKey());
		assertThat(jws.getUnverifiedPayload(), equalTo(jwsPayload));

		assertThat(jws.verifySignature(), is(true));
		assertThat(jws.getPayload(), equalTo(jwsPayload));

		assertThat(jws.getKeyIdHeaderValue(), equalTo(jwk.getKeyId()));
		assertThat(alg, equalTo(jws.getAlgorithmHeaderValue()));

		// can't really verify reproducing ECDSA
	}

	@Test
	public void hmacSha2IntegrityProtection_3_4() throws JoseException {
		String jwkJson = "   {\n" + "     \"kty\": \"oct\",\n" + "     \"kid\": \"018c0ae5-4d9b-471b-bfd6-eef314bc7037\",\n"
				+ "     \"use\": \"sig\",\n" + "     \"k\": \"hJtXIZ2uSN5kbQfbtTNWbpdmhkV8FJG-Onbc6mxCcYg\"\n" + "   }";

		String jwsCompactSerialization = "eyJhbGciOiJIUzI1NiIsImtpZCI6IjAxOGMwYWU1LTRkOWItNDcxYi1iZmQ2LW" + "VlZjMxNGJjNzAzNyJ9" + "."
				+ "SXTigJlzIGEgZGFuZ2Vyb3VzIGJ1c2luZXNzLCBGcm9kbywgZ29pbmcgb3V0IH"
				+ "lvdXIgZG9vci4gWW91IHN0ZXAgb250byB0aGUgcm9hZCwgYW5kIGlmIHlvdSBk"
				+ "b24ndCBrZWVwIHlvdXIgZmVldCwgdGhlcmXigJlzIG5vIGtub3dpbmcgd2hlcm" + "UgeW91IG1pZ2h0IGJlIHN3ZXB0IG9mZiB0by4" + "."
				+ "s0h6KThzkfBBBkLspW1h84VsJZFTsPPqMDA7g1Md7p0";

		String alg = AlgorithmIdentifiers.HMAC_SHA256;

		// verify consuming the JWS
		JsonWebSignature jws = new JsonWebSignature();
		jws.setCompactSerialization(jwsCompactSerialization);
		JsonWebKey jwk = JsonWebKey.Factory.newJwk(jwkJson);
		jws.setKey(jwk.getKey());
		assertThat(jws.verifySignature(), is(true));
		assertThat(jws.getPayload(), equalTo(jwsPayload));
		assertThat(jws.getKeyIdHeaderValue(), equalTo(jwk.getKeyId()));
		assertThat(alg, equalTo(jws.getAlgorithmHeaderValue()));

		// verify reproducing it
		jws = new JsonWebSignature();
		jws.setPayload(jwsPayload);
		jws.setAlgorithmHeaderValue(alg);
		jws.setKeyIdHeaderValue(jwk.getKeyId());
		jws.setKey(jwk.getKey());
		String compactSerialization = jws.getCompactSerialization();
		assertThat(jwsCompactSerialization, equalTo(compactSerialization));
	}

	@Test
	public void detached_3_5() throws JoseException {
		String jwkJsonString = "   {\n" + "     \"kty\": \"oct\",\n" + "     \"kid\": \"018c0ae5-4d9b-471b-bfd6-eef314bc7037\",\n"
				+ "     \"use\": \"sig\",\n" + "     \"k\": \"hJtXIZ2uSN5kbQfbtTNWbpdmhkV8FJG-Onbc6mxCcYg\"\n" + "   }";

		String detachedCs = "eyJhbGciOiJIUzI1NiIsImtpZCI6IjAxOGMwYWU1LTRkOWItNDcxYi1iZmQ2LW" + "VlZjMxNGJjNzAzNyJ9" + "." + "."
				+ "s0h6KThzkfBBBkLspW1h84VsJZFTsPPqMDA7g1Md7p0";

		JsonWebSignature jws = new JsonWebSignature();
		jws.setCompactSerialization(detachedCs);
		JsonWebKey jwk = JsonWebKey.Factory.newJwk(jwkJsonString);
		jws.setKey(jwk.getKey());
		jws.setEncodedPayload(encodedJwsPayload);
		assertThat(jws.verifySignature(), is(true));
		assertThat(jws.getPayload(), equalTo(jwsPayload));

		// verify reproducing it (it's just luck that using the setters for the
		// headers results in the exact same
		// JSON representation of the header)
		jws = new JsonWebSignature();
		jws.setPayload(jwsPayload);
		jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256);
		jws.setKeyIdHeaderValue(jwk.getKeyId());
		jws.setKey(jwk.getKey());
		// To create a detached signature, sign and then concatenate the encoded
		// header, two dots "..", and the encoded signature
		jws.sign();
		String encodedHeader = jws.getHeaders().getEncodedHeader();
		String encodedSignature = jws.getEncodedSignature();
		String reproducedDetachedCs = encodedHeader + ".." + encodedSignature;
		assertThat(detachedCs, is(equalTo(reproducedDetachedCs)));
		assertThat(encodedJwsPayload, is(equalTo(jws.getEncodedPayload())));
	}

	@Test
	public void encryptionRSAv1_5andAES_HMAC_SHA2_4_1() throws JoseException {
		String jwkJsonString = "{\n" + "  \"kty\": \"RSA\",\n" + "  \"kid\": \"frodo.baggins@hobbiton.example\",\n"
				+ "  \"use\": \"enc\",\n" + "  \"n\": \"maxhbsmBtdQ3CNrKvprUE6n9lYcregDMLYNeTAWcLj8NnPU9XIYegT\n"
				+ "      HVHQjxKDSHP2l-F5jS7sppG1wgdAqZyhnWvXhYNvcM7RfgKxqNx_xAHx\n"
				+ "      6f3yy7s-M9PSNCwPC2lh6UAkR4I00EhV9lrypM9Pi4lBUop9t5fS9W5U\n"
				+ "      NwaAllhrd-osQGPjIeI1deHTwx-ZTHu3C60Pu_LJIl6hKn9wbwaUmA4c\n"
				+ "      R5Bd2pgbaY7ASgsjCUbtYJaNIHSoHXprUdJZKUMAzV0WOKPfA6OPI4oy\n"
				+ "      pBadjvMZ4ZAj3BnXaSYsEZhaueTXvZB4eZOAjIyh2e_VOIKVMsnDrJYA\n" + "      VotGlvMQ\",\n" + "  \"e\": \"AQAB\",\n"
				+ "  \"d\": \"Kn9tgoHfiTVi8uPu5b9TnwyHwG5dK6RE0uFdlpCGnJN7ZEi963R7wy\n"
				+ "      bQ1PLAHmpIbNTztfrheoAniRV1NCIqXaW_qS461xiDTp4ntEPnqcKsyO\n"
				+ "      5jMAji7-CL8vhpYYowNFvIesgMoVaPRYMYT9TW63hNM0aWs7USZ_hLg6\n"
				+ "      Oe1mY0vHTI3FucjSM86Nff4oIENt43r2fspgEPGRrdE6fpLc9Oaq-qeP\n"
				+ "      1GFULimrRdndm-P8q8kvN3KHlNAtEgrQAgTTgz80S-3VD0FgWfgnb1PN\n"
				+ "      miuPUxO8OpI9KDIfu_acc6fg14nsNaJqXe6RESvhGPH2afjHqSy_Fd2v\n" + "      pzj85bQQ\",\n"
				+ "  \"p\": \"2DwQmZ43FoTnQ8IkUj3BmKRf5Eh2mizZA5xEJ2MinUE3sdTYKSLtaE\n"
				+ "      oekX9vbBZuWxHdVhM6UnKCJ_2iNk8Z0ayLYHL0_G21aXf9-unynEpUsH\n"
				+ "      7HHTklLpYAzOOx1ZgVljoxAdWNn3hiEFrjZLZGS7lOH-a3QQlDDQoJOJ\n" + "      2VFmU\",\n"
				+ "  \"q\": \"te8LY4-W7IyaqH1ExujjMqkTAlTeRbv0VLQnfLY2xINnrWdwiQ93_V\n"
				+ "      F099aP1ESeLja2nw-6iKIe-qT7mtCPozKfVtUYfz5HrJ_XY2kfexJINb\n"
				+ "      9lhZHMv5p1skZpeIS-GPHCC6gRlKo1q-idn_qxyusfWv7WAxlSVfQfk8\n" + "      d6Et0\",\n"
				+ "  \"dp\": \"UfYKcL_or492vVc0PzwLSplbg4L3-Z5wL48mwiswbpzOyIgd2xHTH\n"
				+ "      QmjJpFAIZ8q-zf9RmgJXkDrFs9rkdxPtAsL1WYdeCT5c125Fkdg317JV\n"
				+ "      RDo1inX7x2Kdh8ERCreW8_4zXItuTl_KiXZNU5lvMQjWbIw2eTx1lpsf\n" + "      lo0rYU\",\n"
				+ "  \"dq\": \"iEgcO-QfpepdH8FWd7mUFyrXdnOkXJBCogChY6YKuIHGc_p8Le9Mb\n"
				+ "      pFKESzEaLlN1Ehf3B6oGBl5Iz_ayUlZj2IoQZ82znoUrpa9fVYNot87A\n"
				+ "      CfzIG7q9Mv7RiPAderZi03tkVXAdaBau_9vs5rS-7HMtxkVrxSUvJY14\n" + "      TkXlHE\",\n"
				+ "  \"qi\": \"kC-lzZOqoFaZCr5l0tOVtREKoVqaAYhQiqIRGL-MzS4sCmRkxm5vZ\n"
				+ "      lXYx6RtE1n_AagjqajlkjieGlxTTThHD8Iga6foGBMaAr5uR1hGQpSc7\n"
				+ "      Gl7CF1DZkBJMTQN6EshYzZfxW08mIO8M6Rzuh0beL6fG9mkDcIyPrBXx\n" + "      2bQ_mM\"\n" + "}";

		String jweCompactSerialization = "eyJhbGciOiJSU0ExXzUiLCJraWQiOiJmcm9kby5iYWdnaW5zQGhvYmJpdG9uLm"
				+ "V4YW1wbGUiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2In0" + "." + "laLxI0j-nLH-_BgLOXMozKxmy9gffy2gTdvqzfTihJBuuzxg0V7yk1WClnQePF"
				+ "vG2K-pvSlWc9BRIazDrn50RcRai__3TDON395H3c62tIouJJ4XaRvYHFjZTZ2G"
				+ "Xfz8YAImcc91Tfk0WXC2F5Xbb71ClQ1DDH151tlpH77f2ff7xiSxh9oSewYrcG"
				+ "TSLUeeCt36r1Kt3OSj7EyBQXoZlN7IxbyhMAfgIe7Mv1rOTOI5I8NQqeXXW8Vl"
				+ "zNmoxaGMny3YnGir5Wf6Qt2nBq4qDaPdnaAuuGUGEecelIO1wx1BpyIfgvfjOh" + "MBs9M8XL223Fg47xlGsMXdfuY-4jaqVw" + "."
				+ "bbd5sTkYwhAIqfHsx8DayA" + "." + "0fys_TY_na7f8dwSfXLiYdHaA2DxUjD67ieF7fcVbIR62JhJvGZ4_FNVSiGc_r"
				+ "aa0HnLQ6s1P2sv3Xzl1p1l_o5wR_RsSzrS8Z-wnI3Jvo0mkpEEnlDmZvDu_k8O"
				+ "WzJv7eZVEqiWKdyVzFhPpiyQU28GLOpRc2VbVbK4dQKPdNTjPPEmRqcaGeTWZV"
				+ "yeSUvf5k59yJZxRuSvWFf6KrNtmRdZ8R4mDOjHSrM_s8uwIFcqt4r5GX8TKaI0"
				+ "zT5CbL5Qlw3sRc7u_hg0yKVOiRytEAEs3vZkcfLkP6nbXdC_PkMdNS-ohP78T2"
				+ "O6_7uInMGhFeX4ctHG7VelHGiT93JfWDEQi5_V9UN1rhXNrYu-0fVMkZAKX3VW" + "i7lzA6BP430m" + "." + "kvKuFBXHe5mQr4lqgobAUg";

		PublicJsonWebKey jwk = PublicJsonWebKey.Factory.newPublicJwk(jwkJsonString);

		// verify that we can decrypt it
		JsonWebEncryption jwe = new JsonWebEncryption();
		jwe.setCompactSerialization(jweCompactSerialization);
		jwe.setKey(jwk.getPrivateKey());
		assertThat(jwePlaintext, equalTo(jwe.getPlaintextString()));

		// verify that we can reproduce it (most of it) from the inputs
		jwe = new JsonWebEncryption();
		jwe.setPlaintext(jwePlaintext);
		jwe.setKey(jwk.getPublicKey());
		jwe.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.RSA1_5);
		jwe.setKeyIdHeaderValue(jwk.getKeyId());
		jwe.setEncryptionMethodHeaderParameter(ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);

		// set the IV and cek per the example (you wouldn't usually do this but
		// it makes the output more deterministic)
		jwe.setEncodedIv("bbd5sTkYwhAIqfHsx8DayA");
		jwe.setEncodedContentEncryptionKey("3qyTVhIWt5juqZUCpfRqpvauwB956MEJL2Rt-8qXKSo");

		// check that the header, iv, ciphertext and tag all match
		String[] deserializedExample = CompactSerializer.deserialize(jweCompactSerialization);
		String[] deserializedResults = CompactSerializer.deserialize(jwe.getCompactSerialization());
		assertThat(deserializedExample[0], equalTo(deserializedResults[0]));
		// RSA v1.5, due to random bytes being used to pad, is nondeterministic
		// so the encrypted key
		// will be different each time in the JWE we're producing and so can't
		// compare to the example
		assertThat(deserializedExample[2], equalTo(deserializedResults[2]));
		assertThat(deserializedExample[3], equalTo(deserializedResults[3]));
		assertThat(deserializedExample[4], equalTo(deserializedResults[4]));
	}

	@Test
	public void encryptionPbes_4_3() throws JoseException {
		String password = "entrap_o_peter_long_credit_tun";

		String exampleCompactSerialization = "eyJhbGciOiJQQkVTMi1IUzUxMitBMjU2S1ciLCJwMnMiOiI4UTFTemluYXNSM3"
				+ "hjaFl6NlpaY0hBIiwicDJjIjo4MTkyLCJjdHkiOiJqd2stc2V0K2pzb24iLCJl" + "bmMiOiJBMTI4Q0JDLUhTMjU2In0" + "."
				+ "pmKVzwf89K3dfkQqbERUTC0F2jGXD6tTQVmtnVpuKUK2J4Xx2RkkLw" + "." + "VBiCzVHNoLiR3F4V82uoTQ" + "."
				+ "23i-Tb1AV4n0WKVSSgcQrdg6GRqsUKxjruHXYsTHAJLZ2nsnGIX86vMXqIi6IR"
				+ "sfywCRFzLxEcZBRnTvG3nhzPk0GDD7FMyXhUHpDjEYCNA_XOmzg8yZR9oyjo6l"
				+ "TF6si4q9FZ2EhzgFQCLO_6h5EVg3vR75_hkBsnuoqoM3dwejXBtIodN84PeqMb"
				+ "6asmas_dpSsz7H10fC5ni9xIz424givB1YLldF6exVmL93R3fOoOJbmk2GBQZL"
				+ "_SEGllv2cQsBgeprARsaQ7Bq99tT80coH8ItBjgV08AtzXFFsx9qKvC982KLKd"
				+ "PQMTlVJKkqtV4Ru5LEVpBZXBnZrtViSOgyg6AiuwaS-rCrcD_ePOGSuxvgtrok"
				+ "AKYPqmXUeRdjFJwafkYEkiuDCV9vWGAi1DH2xTafhJwcmywIyzi4BqRpmdn_N-"
				+ "zl5tuJYyuvKhjKv6ihbsV_k1hJGPGAxJ6wUpmwC4PTQ2izEm0TuSE8oMKdTw8V" + "3kobXZ77ulMwDs4p" + "." + "0HlwodAhOCILG5SQ2LQ9dg";
		String plaintext = "{\"keys\":["
				+ "{\"kty\":\"oct\",\"kid\":\"77c7e2b8-6e13-45cf-8672-617b5b45243a\",\"use\":\"enc\",\"alg\":\"A128GCM\",\"k\":\"XctOhJAkA-pD9Lh7ZgW_2A\"},"
				+ "{\"kty\":\"oct\",\"kid\":\"81b20965-8332-43d9-a468-82160ad91ac8\",\"use\":\"enc\",\"alg\":\"A128KW\",\"k\":\"GZy6sIZ6wl9NJOKB-jnmVQ\"},"
				+ "{\"kty\":\"oct\",\"kid\":\"18ec08e1-bfa9-4d95-b205-2b4dd1d4321d\",\"use\":\"enc\",\"alg\":\"A256GCMKW\",\"k\":\"qC57l_uxcm7Nm3K-ct4GFjx8tM1U8CZ0NLBvdQstiS8\"}]}";

		// verify that we can decrypt it
		JsonWebEncryption jwe = new JsonWebEncryption();
		jwe.setCompactSerialization(exampleCompactSerialization);
		jwe.setKey(new PbkdfKey(password));
		assertThat(plaintext, equalTo(jwe.getPlaintextString()));

		// verify that we can reproduce it from the inputs
		jwe = new JsonWebEncryption();
		jwe.setPlaintext(plaintext);
		jwe.setKey(new PbkdfKey(password));

		jwe.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.PBES2_HS512_A256KW);
		Headers headers = jwe.getHeaders();
		headers.setStringHeaderValue(HeaderParameterNames.PBES2_SALT_INPUT, "8Q1SzinasR3xchYz6ZZcHA");
		headers.setObjectHeaderValue(HeaderParameterNames.PBES2_ITERATION_COUNT, 8192L);
		headers.setStringHeaderValue("cty", "jwk-set+json");
		jwe.setEncryptionMethodHeaderParameter(ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);

		// set the IV and cek per the example (you wouldn't usually do this but
		// it makes the output
		// more deterministic so it can be compared to the example)
		jwe.setEncodedContentEncryptionKey("uwsjJXaBK407Qaf0_zpcpmr1Cs0CC50hIUEyGNEt3m0");
		jwe.setEncodedIv("VBiCzVHNoLiR3F4V82uoTQ");

		assertThat(jwe.getCompactSerialization(), equalTo(exampleCompactSerialization));
	}

	@Test
	public void encryptionPbes_4_3_from_draft_2() throws JoseException {
		// draft -02 used, I think by accident, PBES2-HS256+A128KW, which was
		// changed
		// to PBES2-HS512+A256KW in -03 but the content from -02 is still a
		// useful example so keeping it here
		String password = "entrap_o_peter_long_credit_tun";

		String exampleCompactSerialization = "eyJhbGciOiJQQkVTMi1IUzI1NitBMTI4S1ciLCJwMnMiOiI4UTFTemluYXNSM3"
				+ "hjaFl6NlpaY0hBIiwicDJjIjo4MTkyLCJjdHkiOiJqd2stc2V0K2pzb24iLCJl" + "bmMiOiJBMTI4Q0JDLUhTMjU2In0" + "."
				+ "YKbKLsEoyw_JoNvhtuHo9aaeRNSEhhAW2OVHcuF_HLqS0n6hA_fgCA" + "." + "VBiCzVHNoLiR3F4V82uoTQ" + "."
				+ "23i-Tb1AV4n0WKVSSgcQrdg6GRqsUKxjruHXYsTHAJLZ2nsnGIX86vMXqIi6IR"
				+ "sfywCRFzLxEcZBRnTvG3nhzPk0GDD7FMyXhUHpDjEYCNA_XOmzg8yZR9oyjo6l"
				+ "TF6si4q9FZ2EhzgFQCLO_6h5EVg3vR75_hkBsnuoqoM3dwejXBtIodN84PeqMb"
				+ "6asmas_dpSsz7H10fC5ni9xIz424givB1YLldF6exVmL93R3fOoOJbmk2GBQZL"
				+ "_SEGllv2cQsBgeprARsaQ7Bq99tT80coH8ItBjgV08AtzXFFsx9qKvC982KLKd"
				+ "PQMTlVJKkqtV4Ru5LEVpBZXBnZrtViSOgyg6AiuwaS-rCrcD_ePOGSuxvgtrok"
				+ "AKYPqmXUeRdjFJwafkYEkiuDCV9vWGAi1DH2xTafhJwcmywIyzi4BqRpmdn_N-"
				+ "zl5tuJYyuvKhjKv6ihbsV_k1hJGPGAxJ6wUpmwC4PTQ2izEm0TuSE8oMKdTw8V" + "3kobXZ77ulMwDs4p" + "." + "ALTKwxvAefeL-32NY7eTAQ";

		String plaintext = "{\"keys\":["
				+ "{\"kty\":\"oct\",\"kid\":\"77c7e2b8-6e13-45cf-8672-617b5b45243a\",\"use\":\"enc\",\"alg\":\"A128GCM\",\"k\":\"XctOhJAkA-pD9Lh7ZgW_2A\"},"
				+ "{\"kty\":\"oct\",\"kid\":\"81b20965-8332-43d9-a468-82160ad91ac8\",\"use\":\"enc\",\"alg\":\"A128KW\",\"k\":\"GZy6sIZ6wl9NJOKB-jnmVQ\"},"
				+ "{\"kty\":\"oct\",\"kid\":\"18ec08e1-bfa9-4d95-b205-2b4dd1d4321d\",\"use\":\"enc\",\"alg\":\"A256GCMKW\",\"k\":\"qC57l_uxcm7Nm3K-ct4GFjx8tM1U8CZ0NLBvdQstiS8\"}]}";

		// verify that we can decrypt it
		JsonWebEncryption jwe = new JsonWebEncryption();
		jwe.setCompactSerialization(exampleCompactSerialization);
		jwe.setKey(new PbkdfKey(password));
		assertThat(plaintext, equalTo(jwe.getPlaintextString()));

		// verify that we can reproduce it from the inputs
		jwe = new JsonWebEncryption();
		jwe.setPlaintext(plaintext);
		jwe.setKey(new PbkdfKey(password));

		jwe.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.PBES2_HS256_A128KW);
		Headers headers = jwe.getHeaders();
		headers.setStringHeaderValue(HeaderParameterNames.PBES2_SALT_INPUT, "8Q1SzinasR3xchYz6ZZcHA");
		headers.setObjectHeaderValue(HeaderParameterNames.PBES2_ITERATION_COUNT, 8192L);
		headers.setStringHeaderValue("cty", "jwk-set+json");
		jwe.setEncryptionMethodHeaderParameter(ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);

		// set the IV and cek per the example (you wouldn't usually do this but
		// it makes the output
		// more deterministic so it can be compared to the example)
		jwe.setEncodedContentEncryptionKey("uwsjJXaBK407Qaf0_zpcpmr1Cs0CC50hIUEyGNEt3m0");
		jwe.setEncodedIv("VBiCzVHNoLiR3F4V82uoTQ");

		assertThat(jwe.getCompactSerialization(), equalTo(exampleCompactSerialization));
	}

	@Test
	public void keyAgreementAndAesCbc_4_5() throws JoseException {
		String jwkJson = "{" + "  \"kty\": \"EC\"," + "  \"kid\": \"meriadoc.brandybuck@buckland.example\"," + "  \"use\": \"enc\","
				+ "  \"crv\": \"P-256\"," + "  \"x\": \"Ze2loSV3wrroKUN_4zhwGhCqo3Xhu1td4QjeQ5wIVR0\","
				+ "  \"y\": \"HlLtdXARY_f55A3fnzQbPcm6hgr34Mp8p-nuzQCE0Zw\"," + "  \"d\": \"r_kHyZ-a06rmxM3yESK84r1otSg-aQcVStkRhA-iCM8\""
				+ "}";

		String exampleCompactSerialization = "eyJhbGciOiJFQ0RILUVTIiwia2lkIjoibWVyaWFkb2MuYnJhbmR5YnVja0BidW"
				+ "NrbGFuZC5leGFtcGxlIiwiZXBrIjp7Imt0eSI6IkVDIiwiY3J2IjoiUC0yNTYi"
				+ "LCJ4IjoibVBVS1RfYkFXR0hJaGcwVHBqanFWc1AxclhXUXVfdndWT0hIdE5rZF"
				+ "lvQSIsInkiOiI4QlFBc0ltR2VBUzQ2ZnlXdzVNaFlmR1RUMElqQnBGdzJTUzM0" + "RHY0SXJzIn0sImVuYyI6IkExMjhDQkMtSFMyNTYifQ" + "."
				+ "." + "yc9N8v5sYyv3iGQT926IUg" + "." + "BoDlwPnTypYq-ivjmQvAYJLb5Q6l-F3LIgQomlz87yW4OPKbWE1zSTEFjDfhU9"
				+ "IPIOSA9Bml4m7iDFwA-1ZXvHteLDtw4R1XRGMEsDIqAYtskTTmzmzNa-_q4F_e"
				+ "vAPUmwlO-ZG45Mnq4uhM1fm_D9rBtWolqZSF3xGNNkpOMQKF1Cl8i8wjzRli7-"
				+ "IXgyirlKQsbhhqRzkv8IcY6aHl24j03C-AR2le1r7URUhArM79BY8soZU0lzwI"
				+ "-sD5PZ3l4NDCCei9XkoIAfsXJWmySPoeRb2Ni5UZL4mYpvKDiwmyzGd65KqVw7"
				+ "MsFfI_K767G9C9Azp73gKZD0DyUn1mn0WW5LmyX_yJ-3AROq8p1WZBfG-ZyJ61" + "95_JGG2m9Csg" + "." + "WCCkNa-x4BeB9hIDIfFuhg";

		PublicJsonWebKey jwk = PublicJsonWebKey.Factory.newPublicJwk(jwkJson);

		// verify that we can decrypt it
		JsonWebEncryption jwe = new JsonWebEncryption();
		jwe.setCompactSerialization(exampleCompactSerialization);
		jwe.setKey(jwk.getPrivateKey());
		assertThat(jwePlaintext, equalTo(jwe.getPlaintextString()));
	}

}
