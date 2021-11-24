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

package org.jose4j.jws;

import org.jose4j.jwk.JsonWebKey;
import org.jose4j.lang.JoseException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 */
public class ExamplesFromOpenIdConnectTest
{
    @Test
    public void verifySignedRequestObject() throws JoseException
    {
        // OpenID Connect Core 1.0 - draft 15
        // 5.1.  Passing a Request Object by Value has a JWS JWT with a JWK
        String requestObject = 
                "eyJhbGciOiJSUzI1NiJ9.ew0KICJyZXNwb25zZV90eXBlIjogImNvZGUgaWRfdG9rZW" +
                "4iLA0KICJjbGllbnRfaWQiOiAiczZCaGRSa3F0MyIsDQogInJlZGlyZWN0X3VyaSI6I" +
                "CJodHRwczovL2NsaWVudC5leGFtcGxlLm9yZy9jYiIsDQogInNjb3BlIjogIm9wZW5p" +
                "ZCIsDQogInN0YXRlIjogImFmMGlmanNsZGtqIiwNCiAibm9uY2UiOiAibi0wUzZfV3p" +
                "BMk1qIiwNCiAibWF4X2FnZSI6IDg2NDAwLA0KICJjbGFpbXMiOiANCiAgew0KICAgIn" +
                "VzZXJpbmZvIjogDQogICAgew0KICAgICAiZ2l2ZW5fbmFtZSI6IHsiZXNzZW50aWFsI" +
                "jogdHJ1ZX0sDQogICAgICJuaWNrbmFtZSI6IG51bGwsDQogICAgICJlbWFpbCI6IHsi" +
                "ZXNzZW50aWFsIjogdHJ1ZX0sDQogICAgICJlbWFpbF92ZXJpZmllZCI6IHsiZXNzZW5" +
                "0aWFsIjogdHJ1ZX0sDQogICAgICJwaWN0dXJlIjogbnVsbA0KICAgIH0sDQogICAiaW" +
                "RfdG9rZW4iOiANCiAgICB7DQogICAgICJnZW5kZXIiOiBudWxsLA0KICAgICAiYmlyd" +
                "GhkYXRlIjogeyJlc3NlbnRpYWwiOiB0cnVlfSwNCiAgICAgImFjciI6IHsidmFsdWVz" +
                "IjogWyIyIl19DQogICAgfQ0KICB9DQp9.bOD4rUiQfzh4QPIs_f_R2GVBhNHcc1p2cQ" +
                "TgixB1tsYRs52xW4TO74USgb-nii3RPsLdfoPlsEbJLmtbxG8-TQBHqGAyZxMDPWy3p" +
                "hjeRt9ApDRnLQrjYuvsCj6byu9TVaKX9r1KDFGT-HLqUNlUTpYtCyM2B2rLkWM08ufB" +
                "q9JBCEzzaLRzjevYEPMaoLAOjb8LPuYOYTBqshRMUxy4Z380-FJ2Lc7VSfSu6HcB2nL" +
                "SjiKrrfI35xkRJsaSSmjasMYeDZarYCl7r4o17rFclk5KacYMYgAs-JYFkwab6Dd56Z" +
                "rAzakHt9cExMpg04lQIux56C-Qk6dAsB6W6W91AQ";

        String jwkJson = "{\n" +
                "   \"kty\":\"RSA\",\n" +
                "   \"n\":\"y9Lqv4fCp6Ei-u2-ZCKq83YvbFEk6JMs_pSj76eMkddWRuWX2aBKGHAtKlE5P\n" +
                "        7_vn__PCKZWePt3vGkB6ePgzAFu08NmKemwE5bQI0e6kIChtt_6KzT5OaaXDF\n" +
                "        I6qCLJmk51Cc4VYFaxgqevMncYrzaW_50mZ1yGSFIQzLYP8bijAHGVjdEFgZa\n" +
                "        ZEN9lsn_GdWLaJpHrB3ROlS50E45wxrlg9xMncVb8qDPuXZarvghLL0HzOuYR\n" +
                "        adBJVoWZowDNTpKpk2RklZ7QaBO7XDv3uR7s_sf2g-bAjSYxYUGsqkNA9b3xV\n" +
                "        W53am_UZZ3tZbFTIh557JICWKHlWj5uzeJXaw\",\n" +
                "   \"e\":\"AQAB\"\n" +
                "  }";

        JsonWebKey jwk = JsonWebKey.Factory.newJwk(jwkJson);

        JsonWebSignature jws = new JsonWebSignature();
        jws.setCompactSerialization(requestObject);
        jws.setKey(jwk.getKey());
        assertThat(jws.verifySignature(), is(true));
    }


    @Test
    public void verifyIdTokens() throws JoseException
    {
        // OpenID Connect Core 1.0 - draft 15
        // Appendix A.  Authorization Examples has several singed ID Tokens and a JWK
        String idTokenA2 = "eyJhbGciOiJSUzI1NiJ9.ew0KICJpc3MiOiAiaHR0cDovL3Nlc\n" +
                "    nZlci5leGFtcGxlLmNvbSIsDQogInN1YiI6ICIyNDgyODk3NjEwMDEiLA0KI\n" +
                "    CJhdWQiOiAiczZCaGRSa3F0MyIsDQogIm5vbmNlIjogIm4tMFM2X1d6QTJNa\n" +
                "    iIsDQogImV4cCI6IDEzMTEyODE5NzAsDQogImlhdCI6IDEzMTEyODA5NzAsD\n" +
                "    QogIm5hbWUiOiAiSmFuZSBEb2UiLA0KICJnaXZlbl9uYW1lIjogIkphbmUiL\n" +
                "    A0KICJmYW1pbHlfbmFtZSI6ICJEb2UiLA0KICJnZW5kZXIiOiAiZmVtYWxlI\n" +
                "    iwNCiAiYmlydGhkYXRlIjogIjAwMDAtMTAtMzEiLA0KICJlbWFpbCI6ICJqY\n" +
                "    W5lZG9lQGV4YW1wbGUuY29tIiwNCiAicGljdHVyZSI6ICJodHRwOi8vZXhhb\n" +
                "    XBsZS5jb20vamFuZWRvZS9tZS5qcGciDQp9.Bgdr1pzosIrnnnpIekmJ7ooe\n" +
                "    DbXuA2AkwfMf90Po2TrMcl3NQzUE_9dcr9r8VOuk4jZxNpV5kCu0RwqqF11-\n" +
                "    6pQ2KQx_ys2i0arLikdResxvJlZzSm_UG6-21s97IaXC97vbnTCcpAkokSe8\n" +
                "    Uik6f8-U61zVmCBMJnpvnxEJllfV8fYldo8lWCqlOngScEbFQUh4fzRsH8O3\n" +
                "    Znr20UZib4V4mGZqYPtPDVGTeu8xkty1t0aK-wEhbm6Hi-TQTi4kltJlw47M\n" +
                "    cSVgF_8SswaGcW6Bf_954ir_ddi4Nexo9RBiWu4n3JMNcQvZU5xMPhu-EF-6\n" +
                "    _nJNotp-lbnBUyxTSg";

        String idTokenA3 = "eyJhbGciOiJSUzI1NiJ9.ew0KICJpc3MiOiAiaHR0cDovL3NlcnZlc\n" +
                "    i5leGFtcGxlLmNvbSIsDQogInN1YiI6ICIyNDgyODk3NjEwMDEiLA0KICJhdWQiO\n" +
                "    iAiczZCaGRSa3F0MyIsDQogIm5vbmNlIjogIm4tMFM2X1d6QTJNaiIsDQogImV4c\n" +
                "    CI6IDEzMTEyODE5NzAsDQogImlhdCI6IDEzMTEyODA5NzAsDQogImF0X2hhc2giO\n" +
                "    iAiNzdRbVVQdGpQZnpXdEYyQW5wSzlSUSINCn0.g7UR4IDBNIjoPFV8exQCosUNV\n" +
                "    eh8bNUTeL4wdQp-2WXIWnly0_4ZK0sh4A4uddfenzo4Cjh4wuPPrSw6lMeujYbGy\n" +
                "    zKspJrRYL3iiYWc2VQcl8RKdHPz_G-7yf5enut1YE8v7PhKucPJCRRoobMjqD73f\n" +
                "    1nJNwQ9KBrfh21Ggbx1p8hNqQeeLLXb9b63JD84hVOXwyHmmcVgvZskge-wExwnh\n" +
                "    Ivv_cxTzxIXsSxcYlh3d9hnu0wdxPZOGjT0_nNZJxvdIwDD4cAT_LE5Ae447qB90\n" +
                "    ZF89Nmb0Oj2b1GdGVQEIr8-FXrHlyD827f0N_hLYPdZ73YK6p10qY9oRtMimg";

        String idTokenA4 = "eyJhbGciOiJSUzI1NiJ9.ew0KICJpc3MiOiAiaHR0cDovL3NlcnZlc\n" +
                "    i5leGFtcGxlLmNvbSIsDQogInN1YiI6ICIyNDgyODk3NjEwMDEiLA0KICJhdWQiO\n" +
                "    iAiczZCaGRSa3F0MyIsDQogIm5vbmNlIjogIm4tMFM2X1d6QTJNaiIsDQogImV4c\n" +
                "    CI6IDEzMTEyODE5NzAsDQogImlhdCI6IDEzMTEyODA5NzAsDQogImNfaGFzaCI6I\n" +
                "    CJMRGt0S2RvUWFrM1BrMGNuWHhDbHRBIg0KfQ.dAVXerlNOJ_tqMUysD_k1Q_bRX\n" +
                "    RJbLkTOsCPVxpKUis5V6xMRvtjfRg8gUfPuAMYrKQMEqZZmL87Hxkv6cFKavb4ft\n" +
                "    BUrY2qUnrvqe_bNjVEz89QSdxGmdFwSTgFVGWkDf5dV5eIiRxXfIkmlgCltPNocR\n" +
                "    AyvdNrsWC661rHz5F9MzBho2vgi5epUa_KAl6tK4ksgl68pjZqlBqsWfTbGEsWQX\n" +
                "    Efu664dJkdXMLEnsPUeQQLjMhLH7qpZk2ry0nRx0sS1mRwOM_Q0Xmps0vOkNn284\n" +
                "    pMUpmWEAjqklWITgtVYXOzF4ilbmZK6ONpFyKCpnSkAYtTEuqz-m7MoLCD_A";


        String idTokenA6 = "eyJhbGciOiJSUzI1NiJ9.ew0KICJpc3MiOiAiaHR0cDovL3NlcnZlc\n" +
                "    i5leGFtcGxlLmNvbSIsDQogInN1YiI6ICIyNDgyODk3NjEwMDEiLA0KICJhdWQiO\n" +
                "    iAiczZCaGRSa3F0MyIsDQogIm5vbmNlIjogIm4tMFM2X1d6QTJNaiIsDQogImV4c\n" +
                "    CI6IDEzMTEyODE5NzAsDQogImlhdCI6IDEzMTEyODA5NzAsDQogImF0X2hhc2giO\n" +
                "    iAiNzdRbVVQdGpQZnpXdEYyQW5wSzlSUSIsDQogImNfaGFzaCI6ICJMRGt0S2RvU\n" +
                "    WFrM1BrMGNuWHhDbHRBIg0KfQ.JQthrBsOirujair9aD5gj1Yd5qEv0j4fhLgl8h\n" +
                "    3RaH3soYhwPOiN2Iy_yb7wMCO6I3bPoGJc3zCkpjgUtdB4O2eEhFqXHdwnE4c0oV\n" +
                "    TaTHJi_PdV2ox9g-1ikDB0ckWk0f0SzBd7yM2RoYYxJCiGBQlsSSRQz6ehykonI3\n" +
                "    hLAhXFdpfbK-3_a3HBNKOv_9Mr_JJrz2pqSygk5IBNvwzf1ouVeM91KKvr7EdriK\n" +
                "    N8ysk68fctbFAga1p8rE3cfBOX7Acn4p9QSNpUx0i_x4WHktyKDvH_hLdUw91Fql\n" +
                "    _UOgMP_9h8TYdkAjcq8n1tFzaO7kVaazlZ5SM32J7OSDgNSA";

        String jwkJson = "  {\n" +
                "   \"kty\":\"RSA\",\n" +
                "   \"n\":\"zhEWTBJVTfcUeqnMzOQFMCEVQWOyOUZwP8LrBWh88tKrZyPGCvBkTDp-E2Bzy\n" +
                "        HMQV4pK51Uys2YOwzL9se5THDWMda9rtsCJVcj1V7WaE7wPgl-kIIdWWf4o2g\n" +
                "        6ZszOy_Fp4q0nG3OTtDRCkBu2iEP21j82pRSRrkCBxnzaChflA7KZbI1n_yhK\n" +
                "        txyA7FdA480LaSVZyKApvrKiYhocACSwf0y6CQ-wkEi6mVXRJt1aBSywlLYA0\n" +
                "        8ojp5hkZQ39eCM2k1EdXdhbar998Q9PZTwXA1cfvuGTZbDWxEKLjMKVuKrT1Y\n" +
                "        vs-2NTXhZAW1KjFS_3UwLkDk-w4dVN-x5tDnw\",\n" +
                "   \"e\":\"AQAB\"\n" +
                "  }";

        JsonWebKey jwk = JsonWebKey.Factory.newJwk(jwkJson);

        for (String idToken : new String[] {idTokenA2, idTokenA3, idTokenA4, idTokenA6})
        {
            JsonWebSignature jws = new JsonWebSignature();
            jws.setCompactSerialization(idToken);
            jws.setKey(jwk.getKey());
            assertThat(jws.verifySignature(), is(true));
        }
    }

    @Test
    public void verifyIdTokensWithKid() throws JoseException
    {
        // OpenID Connect Core 1.0 - draft 15 ** with my changes to have a kid per http://lists.openid.net/pipermail/openid-specs-ab/Week-of-Mon-20131104/004310.html**
        // Appendix A.  Authorization Examples has several singed ID Tokens and a JWK
        String idTokenA2 = "eyJraWQiOiIxZTlnZGs3IiwiYWxnIjoiUlMyNTYifQ.ewogImlz\n" +
                "    cyI6ICJodHRwOi8vc2VydmVyLmV4YW1wbGUuY29tIiwKICJzdWIiOiAiMjQ4\n" +
                "    Mjg5NzYxMDAxIiwKICJhdWQiOiAiczZCaGRSa3F0MyIsCiAibm9uY2UiOiAi\n" +
                "    bi0wUzZfV3pBMk1qIiwKICJleHAiOiAxMzExMjgxOTcwLAogImlhdCI6IDEz\n" +
                "    MTEyODA5NzAsCiAibmFtZSI6ICJKYW5lIERvZSIsCiAiZ2l2ZW5fbmFtZSI6\n" +
                "    ICJKYW5lIiwKICJmYW1pbHlfbmFtZSI6ICJEb2UiLAogImdlbmRlciI6ICJm\n" +
                "    ZW1hbGUiLAogImJpcnRoZGF0ZSI6ICIwMDAwLTEwLTMxIiwKICJlbWFpbCI6\n" +
                "    ICJqYW5lZG9lQGV4YW1wbGUuY29tIiwKICJwaWN0dXJlIjogImh0dHA6Ly9l\n" +
                "    eGFtcGxlLmNvbS9qYW5lZG9lL21lLmpwZyIKfQ.rHQjEmBqn9Jre0OLykYNn\n" +
                "    spA10Qql2rvx4FsD00jwlB0Sym4NzpgvPKsDjn_wMkHxcp6CilPcoKrWHcip\n" +
                "    R2iAjzLvDNAReF97zoJqq880ZD1bwY82JDauCXELVR9O6_B0w3K-E7yM2mac\n" +
                "    AAgNCUwtik6SjoSUZRcf-O5lygIyLENx882p6MtmwaL1hd6qn5RZOQ0TLrOY\n" +
                "    u0532g9Exxcm-ChymrB4xLykpDj3lUivJt63eEGGN6DH5K6o33TcxkIjNrCD\n" +
                "    4XB1CKKumZvCedgHHF3IAK4dVEDSUoGlH9z4pP_eWYNXvqQOjGs-rDaQzUHl\n" +
                "    6cQQWNiDpWOl_lxXjQEvQ";

        String idTokenA3 = "eyJraWQiOiIxZTlnZGs3IiwiYWxnIjoiUlMyNTYifQ.ewogIml\n" +
                "    zcyI6ICJodHRwOi8vc2VydmVyLmV4YW1wbGUuY29tIiwKICJzdWIiOiAiMjQ\n" +
                "    4Mjg5NzYxMDAxIiwKICJhdWQiOiAiczZCaGRSa3F0MyIsCiAibm9uY2UiOiA\n" +
                "    ibi0wUzZfV3pBMk1qIiwKICJleHAiOiAxMzExMjgxOTcwLAogImlhdCI6IDE\n" +
                "    zMTEyODA5NzAsCiAiYXRfaGFzaCI6ICI3N1FtVVB0alBmeld0RjJBbnBLOVJ\n" +
                "    RIgp9.F9gRev0Dt2tKcrBkHy72cmRqnLdzw9FLCCSebV7mWs7o_sv2O5s6zM\n" +
                "    ky2kmhHTVx9HmdvNnx9GaZ8XMYRFeYk8L5NZ7aYlA5W56nsG1iWOou_-gji0\n" +
                "    ibWIuuf4Owaho3YSoi7EvsTuLFz6tq-dLyz0dKABMDsiCmJ5wqkPUDTE3QTX\n" +
                "    jzbUmOzUDli-gCh5QPuZAq0cNW3pf_2n4zpvTYtbmj12cVcxGIMZby7TMWES\n" +
                "    RjQ9_o3jvhVNcCGcE0KAQXejhA1ocJhNEvQNqMFGlBb6_0RxxKjDZ-Oa329e\n" +
                "    GDidOvvp0h5hoES4a8IuGKS7NOcpp-aFwp0qVMDLI-Xnm-Pg";

        String idTokenA4 = "eyJraWQiOiIxZTlnZGs3IiwiYWxnIjoiUlMyNTYifQ.ewogIml\n" +
                "    zcyI6ICJodHRwOi8vc2VydmVyLmV4YW1wbGUuY29tIiwKICJzdWIiOiAiMjQ\n" +
                "    4Mjg5NzYxMDAxIiwKICJhdWQiOiAiczZCaGRSa3F0MyIsCiAibm9uY2UiOiA\n" +
                "    ibi0wUzZfV3pBMk1qIiwKICJleHAiOiAxMzExMjgxOTcwLAogImlhdCI6IDE\n" +
                "    zMTEyODA5NzAsCiAiYXRfaGFzaCI6ICI3N1FtVVB0alBmeld0RjJBbnBLOVJ\n" +
                "    RIgp9.F9gRev0Dt2tKcrBkHy72cmRqnLdzw9FLCCSebV7mWs7o_sv2O5s6zM\n" +
                "    ky2kmhHTVx9HmdvNnx9GaZ8XMYRFeYk8L5NZ7aYlA5W56nsG1iWOou_-gji0\n" +
                "    ibWIuuf4Owaho3YSoi7EvsTuLFz6tq-dLyz0dKABMDsiCmJ5wqkPUDTE3QTX\n" +
                "    jzbUmOzUDli-gCh5QPuZAq0cNW3pf_2n4zpvTYtbmj12cVcxGIMZby7TMWES\n" +
                "    RjQ9_o3jvhVNcCGcE0KAQXejhA1ocJhNEvQNqMFGlBb6_0RxxKjDZ-Oa329e\n" +
                "    GDidOvvp0h5hoES4a8IuGKS7NOcpp-aFwp0qVMDLI-Xnm-Pg";


        String idTokenA6 = "eyJraWQiOiIxZTlnZGs3IiwiYWxnIjoiUlMyNTYifQ.ewogIml\n" +
                "    zcyI6ICJodHRwOi8vc2VydmVyLmV4YW1wbGUuY29tIiwKICJzdWIiOiAiMjQ\n" +
                "    4Mjg5NzYxMDAxIiwKICJhdWQiOiAiczZCaGRSa3F0MyIsCiAibm9uY2UiOiA\n" +
                "    ibi0wUzZfV3pBMk1qIiwKICJleHAiOiAxMzExMjgxOTcwLAogImlhdCI6IDE\n" +
                "    zMTEyODA5NzAsCiAiY19oYXNoIjogIkxEa3RLZG9RYWszUGswY25YeENsdEE\n" +
                "    iCn0.XW6uhdrkBgcGx6zVIrCiROpWURs-4goO1sKA4m9jhJIImiGg5muPUcN\n" +
                "    egx6sSv43c5DSn37sxCRrDZZm4ZPBKKgtYASMcE20SDgvYJdJS0cyuFw7Ijp\n" +
                "    _7WnIjcrl6B5cmoM6ylCvsLMwkoQAxVublMwH10oAxjzD6NEFsu9nipkszWh\n" +
                "    sPePf_rM4eMpkmCbTzume-fzZIi5VjdWGGEmzTg32h3jiex-r5WTHbj-u5HL\n" +
                "    7u_KP3rmbdYNzlzd1xWRYTUs4E8nOTgzAUwvwXkIQhOh5TPcSMBYy6X3E7-_\n" +
                "    gr9Ue6n4ND7hTFhtjYs3cjNKIA08qm5cpVYFMFMG6PkhzLQ";

        String jwkJson = "  {\n" +
                "   \"kty\":\"RSA\",\n" +
                "   \"kid\":\"1e9gdk7\",\n" +
                "   \"n\":\"w7Zdfmece8iaB0kiTY8pCtiBtzbptJmP28nSWwtdjRu0f2GFpajvWE4VhfJA\n" +
                "        jEsOcwYzay7XGN0b-X84BfC8hmCTOj2b2eHT7NsZegFPKRUQzJ9wW8ipn_aD\n" +
                "        JWMGDuB1XyqT1E7DYqjUCEOD1b4FLpy_xPn6oV_TYOfQ9fZdbE5HGxJUzeku\n" +
                "        GcOKqOQ8M7wfYHhHHLxGpQVgL0apWuP2gDDOdTtpuld4D2LK1MZK99s9gaSj\n" +
                "        RHE8JDb1Z4IGhEcEyzkxswVdPndUWzfvWBBWXWxtSUvQGBRkuy1BHOa4sP6F\n" +
                "        KjWEeeF7gm7UMs2Nm2QUgNZw6xvEDGaLk4KASdIxRQ\",\n" +
                "   \"e\":\"AQAB\"\n" +
                "  }";

        JsonWebKey jwk = JsonWebKey.Factory.newJwk(jwkJson);

        for (String idToken : new String[] {idTokenA2, idTokenA3, idTokenA4, idTokenA6})
        {
            JsonWebSignature jws = new JsonWebSignature();
            jws.setCompactSerialization(idToken);
            jws.setKey(jwk.getKey());
            assertThat(jws.verifySignature(), is(true));
        }
    }
}
