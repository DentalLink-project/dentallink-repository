package com.dentallink.domain.auth.dto.response;

public record TokenResponse (
        String token
){
    public static TokenResponse of(String token){
        return new TokenResponse(token);
    }
}
