package com.epoint.authenticator.identity;

public class OAuthIdentity extends Identity {

    private String grantType;

    public String getGrantType() {
        return grantType;
    }

    public boolean isCode() {
        return "authorization_code".equals(getGrantType());
    }

    public boolean isPassword() {
        return "password".equals(getGrantType());
    }
}
