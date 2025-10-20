package com.dentallink.domain.qna.dto.response;

import com.dentallink.domain.qna.entity.Question;

import java.time.LocalDateTime;
import java.util.List;

public class QuestionResponseDto {

    public record QuestionResponse(
            Long id,
            Long userId,
            Long hospitalId,
            String title,
            String content,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<AnswerResponseDto.AnswerResponse> answers
    ) {
        public static QuestionResponse from(Question question) {
            return new QuestionResponse(
                    question.getId(),
                    question.getUserId(),
                    question.getHospitalId(),
                    question.getTitle(),
                    question.getContent(),
                    question.getCreatedAt(),
                    question.getUpdatedAt(),
                    question.getAnswerList().stream()
                            .map(AnswerResponseDto.AnswerResponse::from)
                            .toList()
            );
        }
    }
}
