package com.dentallink.domain.qna.controller;

import com.dentallink.common.response.ApiResponse;
import com.dentallink.domain.qna.dto.request.QuestionRequestDto;
import com.dentallink.domain.qna.dto.response.QuestionResponseDto;
import com.dentallink.domain.qna.service.QuestionExternalService;
import com.dentallink.domain.user.dto.security.AuthUser;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionExternalService questionExternalService;

    // 문의 등록 API question create api
    @PostMapping
    public ResponseEntity<ApiResponse<QuestionResponseDto.QuestionResponse>> createQuestion(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody QuestionRequestDto.QuestionCreateRequest req
    ) {
        Long userId = authUser.getUserId();     // 인증된 사용자 ID 사용
        return ApiResponse.created(
                questionExternalService.create(userId, req.hospitalId(), req.title(), req.content()),
                "문의 등록 완료"
        );
    }

    // 문의 조회 APi question read api
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionResponseDto.QuestionResponse>> getQuestion(
            @PathVariable Long id
    ) {
        return ApiResponse.success(
                QuestionResponseDto.QuestionResponse.from(questionExternalService.getWithAnswers()),
                "문의 조회 완료"
        );
    }

    // 문의 수정 API question update api
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateQuestion(
            // todo 보안 취약점 확인
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody QuestionRequestDto.QuestionUpdateRequest req
    ) {
        Long userId = authUser.getUserId();     // 인증된 사용자 ID 사용
        questionExternalService.update(id, userId, req.title(), req.content());
        return ApiResponse.success(null, "문의 수정 완료");
    }

    // 문의 삭제 API question delete api
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            // todo 보안 취약점 확인
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        Long userId = authUser.getUserId();     // 인증된 사용자 ID 사용
        questionExternalService.delete(id, userId);
        return ApiResponse.deleteSuccess("문의 삭제 완료");
    }
}
