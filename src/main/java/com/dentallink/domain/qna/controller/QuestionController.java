package com.dentallink.domain.qna.controller;

import com.dentallink.common.response.ApiResponse;
import com.dentallink.domain.qna.dto.request.QuestionRequestDto;
import com.dentallink.domain.qna.dto.response.QuestionResponseDto;
import com.dentallink.domain.qna.service.QuestionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;

    // 질문 등록 API question create api
    @PostMapping
    public ResponseEntity<ApiResponse<QuestionResponseDto.QuestionResponse>> createQuestion(
            /**
             * userId를 @RequestParam으로 전달받는 것은 심각한 보안 취약점을 야기할 수 있습니다.
             * 악의적인 사용자가 다른 사용자의 ID를 사용하여 질문을 생성, 수정, 삭제할 수 있습니다.
             * 현재 인증된 사용자의 ID는 Spring Security의 AuthenticationPrincipal 등을 통해 보안 컨텍스트에서 안전하게 얻어와야 합니다.
             * 이 문제는 updateQuestion, deleteQuestion 메소드에도 동일하게 적용됩니다.
             * */
            @RequestParam Long userId,
            @RequestBody QuestionRequestDto.QuestionCreateRequest req
    ) {
        return ApiResponse.created(
                questionService.create(userId, req.hospitalId(), req.title(), req.content()),
                "문의 등록 완료"
        );
    }

    // 질문 조회 APi question read api
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionResponseDto.QuestionResponse>> getQuestion(
            @PathVariable Long id
    ) {
        return ApiResponse.success(
                QuestionResponseDto.QuestionResponse.from(questionService.getWithAnswers(id)),
                "문의 조회 완료"
        );
    }

    // 질문 수정 API question update api
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateQuestion(
            // todo 보안 취약점 확인
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestBody QuestionRequestDto.QuestionUpdateRequest req
    ) {
        questionService.update(id, userId, req.title(), req.content());
        return ApiResponse.success(null, "문의 수정 완료");
    }

    // 질문 삭제 API question delete api
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            // todo 보안 취약점 확인
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        questionService.delete(id, userId);
        return ApiResponse.deleteSuccess("문의 삭제 완료");
    }
}
