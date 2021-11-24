package org.jose4j.jwx;

import org.jose4j.json.JsonUtil;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jwt.ReservedClaimNames;
import org.jose4j.lang.JoseException;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 */
public class HeadersTest
{
    @Test
    public void testRoundTripJwkHeader() throws JoseException
    {
        Headers headers = new Headers();

        String ephemeralJwkJson = "\n{\"kty\":\"EC\",\n" +
                " \"crv\":\"P-256\",\n" +
                " \"x\":\"gI0GAILBdu7T53akrFmMyGcsF3n5dO7MmwNBHKW5SV0\",\n" +
                " \"y\":\"SLW_xSffzlPWrHEVI30DHM_4egVwt3NQqeUD7nMFpps\",\n" +
                " \"d\":\"0_NxaRPUMQoAJt50Gz8YiTr8gRTwyEaCumd-MToTmIo\"\n" +
                "}";
        PublicJsonWebKey ephemeralJwk = PublicJsonWebKey.Factory.newPublicJwk(ephemeralJwkJson);

        String name = "jwk";
        headers.setJwkHeaderValue(name, ephemeralJwk);

        JsonWebKey jwk = headers.getJwkHeaderValue(name);

        assertThat(ephemeralJwk.getKey(), is(equalTo(jwk.getKey())));

        String encodedHeader = headers.getEncodedHeader();

        Headers parsedHeaders = new Headers();
        parsedHeaders.setEncodedHeader(encodedHeader);

        JsonWebKey jwkFromParsed = parsedHeaders.getJwkHeaderValue(name);
        assertThat(ephemeralJwk.getKey(), is(equalTo(jwkFromParsed.getKey())));
    }

    @Test
    public void multiValueHeader()  throws JoseException
    {
        // https://bitbucket.org/b_c/jose4j/issue/2/ - setHeader should have an overload that accepts a String array
        // which it doesn't but you can do it this way
        Headers headers = new Headers();
        headers.setStringHeaderValue(ReservedClaimNames.ISSUER, "me");
        headers.setObjectHeaderValue(ReservedClaimNames.AUDIENCE, Arrays.asList("you", "them"));

        Map<String,Object> map = JsonUtil.parseJson(headers.getFullHeaderAsJsonString());
        assertThat(map.get(ReservedClaimNames.AUDIENCE), is(instanceOf(List.class)));
    }
}
