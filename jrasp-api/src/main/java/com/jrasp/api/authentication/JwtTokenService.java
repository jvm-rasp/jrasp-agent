package com.jrasp.api.authentication;

public interface JwtTokenService {

    String generateToken(String payloadStr);

    boolean verifyToken(String token);

    PayloadDto getDefaultPayloadDto(String username);
}