package com.dentallink.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@AllArgsConstructor
@Data
public class JwtToken {
    private String grantType;
    private String accessToken;
    private String refreshToken;

    public static JwtToken of(String accessToken, String refreshToken) {
        return new JwtToken("Bearer", accessToken, refreshToken);
    }
}
