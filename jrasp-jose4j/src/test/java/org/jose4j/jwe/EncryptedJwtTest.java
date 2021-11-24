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

package org.jose4j.jwe;

import org.jose4j.keys.ExampleRsaJwksFromJwe;
import org.jose4j.lang.JoseException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 *
 */
public class EncryptedJwtTest
{
    @Test   
    public void exampleEncryptedJwtFromJwtA1() throws JoseException
    {
        // http://tools.ietf.org/html/draft-ietf-oauth-json-web-token-19#appendix-A.1
        String jwt = "eyJhbGciOiJSU0ExXzUiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2In0." +
            "QR1Owv2ug2WyPBnbQrRARTeEk9kDO2w8qDcjiHnSJflSdv1iNqhWXaKH4MqAkQtM" +
            "oNfABIPJaZm0HaA415sv3aeuBWnD8J-Ui7Ah6cWafs3ZwwFKDFUUsWHSK-IPKxLG" +
            "TkND09XyjORj_CHAgOPJ-Sd8ONQRnJvWn_hXV1BNMHzUjPyYwEsRhDhzjAD26ima" +
            "sOTsgruobpYGoQcXUwFDn7moXPRfDE8-NoQX7N7ZYMmpUDkR-Cx9obNGwJQ3nM52" +
            "YCitxoQVPzjbl7WBuB7AohdBoZOdZ24WlN1lVIeh8v1K4krB8xgKvRU8kgFrEn_a" +
            "1rZgN5TiysnmzTROF869lQ." +
            "AxY8DCtDaGlsbGljb3RoZQ." +
            "MKOle7UQrG6nSxTLX6Mqwt0orbHvAKeWnDYvpIAeZ72deHxz3roJDXQyhxx0wKaM" +
            "HDjUEOKIwrtkHthpqEanSBNYHZgmNOV7sln1Eu9g3J8." +
            "fiK51VwhsxJ-siBMR-YFiA";

        JsonWebEncryption jwe = new JsonWebEncryption();
        jwe.setCompactSerialization(jwt);
        jwe.setKey(ExampleRsaJwksFromJwe.APPENDIX_A_2.getPrivateKey());
        String expectedPayload = "{\"iss\":\"joe\",\r\n \"exp\":1300819380,\r\n \"http://example.com/is_root\":true}";
        assertThat(jwe.getPayload(), equalTo(expectedPayload));
    }
}
