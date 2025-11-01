package com.dentallink.common.utility;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.dentallink.domain.user.enums.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;


@Slf4j(topic = "JwtProvider")
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    // Header KEY 값
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String REFRESH_TOKEN_HEADER = "Refresh-Token";
    // 사용자 권한 값의 KEY
    public static final String AUTHORIZATION_KEY = "auth";
    // Token 식별자
    public static final String BEARER_PREFIX = "Bearer ";

    // yml에 작성한 secret key 값을 가져옵니다.
    @Value("${jwt.secret.key}") // secretKey
    private String secretKey;
    private SecretKey key;

    // 만료 시간
    @Value("${jwt.access-token-expiration}")
    private long ACCESS_TOKEN_EXPIRATION_TIME;            // 1시간
    @Value("${jwt.refresh-token-expiration}")
    private long REFRESH_TOKEN_EXPIRATION_TIME; // 14일(2주)

    // 입력된 String secretKey를 SecretKey 객체로 만들어 key에 저장합니다.
    // PostConstruct를 통해 이 Bean이 주입된 후 초기화 합니다. (초기화를 위한 메서드)
    @PostConstruct
    public void init() {
        byte[] bytes = Base64.getDecoder().decode(secretKey);
        key = Keys.hmacShaKeyFor(bytes);
    }

    // accessToken 생성
    public String createAccessToken(Long userId, String email, UserRole role) {
        Date now = new Date(); // 현재 시간
        return BEARER_PREFIX + Jwts.builder()
                .subject(String.valueOf(userId)) // 사용자 식별자값(ID)
                .claim("email", email)
                .claim(AUTHORIZATION_KEY, role) // 사용자 권한
                .expiration(new Date(now.getTime() + ACCESS_TOKEN_EXPIRATION_TIME)) // 만료 시간
                .issuedAt(now) // 발급일
                .signWith(key) // 암호화 알고리즘
                .compact();
    }

    // refreshToken 생성
    public String createRefreshToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .expiration(new Date(now.getTime() + REFRESH_TOKEN_EXPIRATION_TIME))
                .issuedAt(now)
                .signWith(key)
                .compact();
    }

    // header 에서 JWT 가져오기
    public String getJwtFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    // token 에서 사용자 정보를 가져옵니다.
    public Claims getUserInfoFromToken(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
