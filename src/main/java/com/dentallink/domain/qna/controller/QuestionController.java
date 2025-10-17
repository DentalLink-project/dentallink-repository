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
    @PostMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionResponseDto>> createQuestion(
            @RequestParam Long userId,
            @RequestBody QuestionRequestDto.Create req
    ) {
        return ApiResponse.created(
                questionService.create(userId, req.hospitalId(), req.title(), req.content()),
                "문의 등록 완료"
        );
    }

    // 질문 조회 APi question read api
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionResponseDto>> getQuestion(@PathVariable Long id) {
        return ApiResponse.success(
                QuestionResponseDto.from(questionService.getWithAnswers(id)),
                "문의 조회 완료"
        );
    }

    // 질문 수정 API question update api
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateQuestion(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestBody QuestionRequestDto.Update req
    ) {
        questionService.update(id, userId, req.title(), req.content());
        return ApiResponse.success(null, "문의 수정 완료");
    }

    // 질문 삭제 API question delete api
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        questionService.delete(id, userId);
        return ApiResponse.deleteSuccess("문의 삭제 완료");
    }
}
