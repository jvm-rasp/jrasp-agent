package org.jose4j.jwx;

import org.jose4j.base64url.Base64Url;
import org.jose4j.json.JsonUtil;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.lang.JoseException;
import org.jose4j.lang.JsonHelp;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 */
public class Headers
{
    protected Base64Url base64url = new Base64Url();

    private Map<String, Object> headerMap = new LinkedHashMap<String, Object>();
    private String header;
    private String encodedHeader;

    public String getFullHeaderAsJsonString()
    {
        if (header == null)
        {
            header = JsonUtil.toJson(headerMap);
        }
        return header;
    }

    public String getEncodedHeader()
    {
        if (encodedHeader == null)
        {
            String headerAsString = getFullHeaderAsJsonString();
            encodedHeader = base64url.base64UrlEncodeUtf8ByteRepresentation(headerAsString);
        }
        return encodedHeader;
    }

    public void setStringHeaderValue(String name, String value)
    {
        setObjectHeaderValue(name, value);
    }

    public void setObjectHeaderValue(String name, Object value)
    {
        headerMap.put(name, value);
        this.header = null;
        this.encodedHeader = null;
    }

    public void setJwkHeaderValue(String name, JsonWebKey jwk)
    {
        Map<String, Object> jwkParams = jwk.toParams(JsonWebKey.OutputControlLevel.PUBLIC_ONLY);
        setObjectHeaderValue(name, jwkParams);
    }

    public String getStringHeaderValue(String headerName)
    {
        return JsonHelp.getString(headerMap, headerName);
    }

    public Long getLongHeaderValue(String headerName)
    {
        return JsonHelp.getLong(headerMap, headerName);
    }

    public Object getObjectHeaderValue(String name)
    {
        return headerMap.get(name);
    }

    public JsonWebKey getJwkHeaderValue(String name) throws JoseException
    {
        Object objectHeaderValue = getObjectHeaderValue(name);
        Map<String, Object> jwkParams = (Map<String, Object>) objectHeaderValue;
        return JsonWebKey.Factory.newJwk(jwkParams);
    }

    public void setFullHeaderAsJsonString(String header) throws JoseException
    {
        this.encodedHeader = null;
        this.header = header;
        headerMap = JsonUtil.parseJson(header);
    }

    void setEncodedHeader(String encodedHeader) throws JoseException
    {
        this.encodedHeader = encodedHeader;
        setFullHeaderAsJsonString(base64url.base64UrlDecodeToUtf8String(this.encodedHeader));
    }
}
