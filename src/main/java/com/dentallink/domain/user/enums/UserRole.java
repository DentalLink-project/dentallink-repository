package com.dentallink.domain.user.enums;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.domain.user.exception.UserErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum UserRole {

    ROLE_USER(Authority.USER),
    ROLE_HOSPITAL(Authority.HOSPITAL),
    ROLE_ADMIN(Authority.ADMIN);

    private final String userRole;

    public static UserRole of(String role) {
        return Arrays.stream(UserRole.values())
                .filter(r -> r.name().equalsIgnoreCase(role))
                .findFirst()
                .orElseThrow(() -> new GlobalException(UserErrorCode.BAD_USER_ROLE));
    }

    public static class Authority {
        public static final String USER = "ROLE_USER";
        public static final String HOSPITAL = "ROLE_HOSPITAL";
        public static final String ADMIN = "ROLE_ADMIN";
    }
}
