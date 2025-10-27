package com.dentallink.domain.qna.controller;

import com.dentallink.common.response.CommonApiResponse;
import com.dentallink.domain.qna.dto.request.AnswerRequestDto;
import com.dentallink.domain.qna.dto.response.AnswerResponseDto;
import com.dentallink.domain.qna.service.AnswerService;
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

    private final AnswerService answerService;

    // 답변 등록 API answer create api
    @PostMapping
    public ResponseEntity<CommonApiResponse<AnswerResponseDto.AnswerResponse>> createAnswer(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody AnswerRequestDto.AnswerCreateRequest req
    ) {
        Long responderId = authUser.getUserId();    // 인증된 사용자 ID 사용
        return CommonApiResponse.created(
                answerService.create(req.questionId(), responderId, req.content()),
                "답변 등록 완료"
        );
    }

    // 답변 조회 API answer read api
    @GetMapping("/{id}")
    public ResponseEntity<CommonApiResponse<AnswerResponseDto.AnswerResponse>> getAnswer(
            @PathVariable Long id
    ) {
        return CommonApiResponse.success(
                AnswerResponseDto.AnswerResponse.from(answerService.get(id)),
                "답변 조회 완료"
        );
    }

    // 답변 수정 API answer update api
    @PatchMapping("/{id}")
    public ResponseEntity<CommonApiResponse<Void>> updateAnswer(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody AnswerRequestDto.AnswerUpdateRequest req
    ) {
        Long responderId = authUser.getUserId();    // 인증된 사용자 ID 사용
        answerService.update(id, responderId, req.content());
        return CommonApiResponse.success(null, "답변 수정 완료");
    }

    // 답변 삭제 API answer delete api
    @DeleteMapping("/{id}")
    public ResponseEntity<CommonApiResponse<Void>> deleteAnswer(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        Long responderId = authUser.getUserId();    // 인증된 사용자 ID 사용
        answerService.delete(id, responderId);
        return CommonApiResponse.deleteSuccess("답변 삭제 완료");
    }
}
