package com.dentallink.domain.user.dto.request;

import jakarta.validation.constraints.Pattern;

public record UserUpdatePasswordRequest(

        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
                message = "비밀번호는 8~20글자여야 하며, 대소문자, 숫자, 특수문자를 포함해야 합니다."
        )
        String newPassword,
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
                message = "비밀번호는 8~20글자여야 하며, 대소문자, 숫자, 특수문자를 포함해야 합니다."
        )
        String oldPassword
) {
    public static UserUpdatePasswordRequest of(String newPassword, String oldPassword) {
        return new UserUpdatePasswordRequest(newPassword, oldPassword);
    }
}
