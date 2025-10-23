package com.dentallink.domain.qna.controller;

import com.dentallink.common.response.ApiResponse;
import com.dentallink.domain.qna.dto.request.AnswerRequestDto;
import com.dentallink.domain.qna.dto.response.AnswerResponseDto;
import com.dentallink.domain.qna.service.AnswerExternalService;
import com.dentallink.domain.user.dto.security.AuthUser;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/answers")
public class AnswerController {

    private final AnswerExternalService answerExternalService;

    // 답변 등록 API answer create api
    @PostMapping
    public ResponseEntity<ApiResponse<AnswerResponseDto.AnswerResponse>> createAnswer(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody AnswerRequestDto.AnswerCreateRequest req
    ) {
        Long responderId = authUser.getUserId();    // 인증된 사용자 ID 사용
        return ApiResponse.created(
                answerExternalService.create(req.questionId(), responderId, req.content()),
                "답변 등록 완료"
        );
    }

    // 답변 조회 API answer read api
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AnswerResponseDto.AnswerResponse>> getAnswer(
            @PathVariable Long id
    ) {
        return ApiResponse.success(
                AnswerResponseDto.AnswerResponse.from(answerExternalService.get(id)),
                "답변 조회 완료"
        );
    }

    // 답변 수정 API answer update api
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateAnswer(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody AnswerRequestDto.AnswerUpdateRequest req
    ) {
        Long responderId = authUser.getUserId();    // 인증된 사용자 ID 사용
        answerExternalService.update(id, responderId, req.content());
        return ApiResponse.success(null, "답변 수정 완료");
    }

    // 답변 삭제 API answer delete api
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAnswer(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        Long responderId = authUser.getUserId();    // 인증된 사용자 ID 사용
        answerExternalService.delete(id, responderId);
        return ApiResponse.deleteSuccess("답변 삭제 완료");
    }
}
