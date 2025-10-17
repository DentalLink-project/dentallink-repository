package com.dentallink.qna.dto.response;

import com.dentallink.qna.entity.Answer;
import com.dentallink.qna.entity.Question;

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
            List<AnswerResponseDto> answerResponseDto
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
                            .map(AnswerResponseDto::from)
                            .toList()
            );
        }
    }

    public record AnswerResponseDto(
            Long id,
            Long responderId,
            String content,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static AnswerResponseDto from(Answer answer) {
            return new AnswerResponseDto(
                    answer.getId(),
                    answer.getResponderId(),
                    answer.getContent(),
                    answer.getCreatedAt(),
                    answer.getUpdatedAt()
            );
        }
    }
}
