package com.dentallink.domain.qna.controller;

import com.dentallink.common.response.ApiResponse;
import com.dentallink.domain.qna.dto.request.AnswerRequestDto;
import com.dentallink.domain.qna.dto.response.AnswerResponseDto;
import com.dentallink.domain.qna.service.AnswerService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/answers")
public class AnswerController {

    private final AnswerService answerService;

    // 답변 등록 API answer create api
    @PostMapping
    public ResponseEntity<ApiResponse<AnswerResponseDto.AnswerResponse>> createAnswer(
            /**
             * responderId를 @RequestParam으로 직접 전달받는 것은 심각한 보안 취약점을 유발할 수 있습니다.
             * 이를 통해 다른 사용자의 ID로 답변을 등록하는 등의 어뷰징이 가능해집니다.
             * 사용자 ID는 Spring Security의 AuthenticationPrincipal 어노테이션 등을 사용하여 현재 인증된 사용자의 정보를 가져와야 합니다.
             * 이 문제는 updateAnswer와 deleteAnswer 메소드에도 동일하게 적용됩니다.
             * */
            @RequestParam Long responderId,
            @Valid @RequestBody AnswerRequestDto.AnswerCreateRequest req
    ) {
        return ApiResponse.created(
                answerService.create(req.questionId(), responderId, req.content()),
                "답변 등록 완료"
        );
    }

    // 답변 조회 API answer read api
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AnswerResponseDto.AnswerResponse>> getAnswer(
            @PathVariable Long id
    ) {
        return ApiResponse.success(
                AnswerResponseDto.AnswerResponse.from(answerService.get(id)),
                "답변 조회 완료"
        );
    }

    // 답변 수정 API answer update api
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateAnswer(
            @PathVariable Long id,
            @RequestParam Long responderId,
            @RequestBody AnswerRequestDto.AnswerUpdateRequest req
    ) {
        answerService.update(id, responderId, req.content());
        return ApiResponse.success(null, "답변 수정 완료");
    }

    // 답변 삭제 API answer delete api
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAnswer(
            @PathVariable Long id,
            @RequestParam Long responderId
    ) {
        answerService.delete(id, responderId);
        return ApiResponse.deleteSuccess("답변 삭제 완료");
    }
}
