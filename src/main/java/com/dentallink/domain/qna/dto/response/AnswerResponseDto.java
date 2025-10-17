package com.dentallink.domain.qna.dto.response;

import com.dentallink.domain.qna.entity.Answer;

import java.time.LocalDateTime;

public class AnswerResponseDto {

    public record AnswerResponse(
            Long id,
            Long questionId,
            Long responderId,
            String content,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static AnswerResponse from(Answer answer) {
            return new AnswerResponse(
                    answer.getId(),
                    answer.getQuestion().getId(),
                    answer.getResponderId(),
                    answer.getContent(),
                    answer.getCreatedAt(),
                    answer.getUpdatedAt()
            );
        }
    }
}
