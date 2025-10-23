package com.dentallink.domain.qna.service;

import com.dentallink.domain.qna.dto.response.QuestionResponseDto;
import com.dentallink.domain.qna.entity.Question;
import com.dentallink.domain.qna.exception.QnaErrorCode;
import com.dentallink.domain.qna.exception.QnaException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class QuestionExternalService {

    private final QuestionInternalService questionInternalService;

    // 문의 등록
    public QuestionResponseDto.QuestionResponse create(Long userId, Long hospitalId, String title, String content) {
        Question saved = questionInternalService.save(Question.of(userId, hospitalId, title, content));
        return QuestionResponseDto.QuestionResponse.from(saved);
    }

    // todo questionInternalService.getQuestion(questionId); 단순화하기
    // 문의 단건 조회
    @Transactional(readOnly = true)
    public Question get(Long questionId) {
        return questionInternalService.getQuestion(questionId);
    }

    // 답변과 함께 문의 전체 조회
    @Transactional(readOnly = true)
    public Question getWithAnswers() {
        return questionInternalService.getQuestions().stream()
                .findFirst()    // .findFirst() 사용하는 이유 : 문의가 있다면 첫 번째 문의를 반환
                .orElseThrow(() -> new QnaException(QnaErrorCode.QUESTION_NOT_FOUND));
    }

    // todo questionInternalService.getQuestion(questionId); 단순화하기
    // 문의 수정
    public void update(Long questionId, Long userId, String title, String content) {
        Question question = questionInternalService.getQuestion(questionId);
        // 본인 문의만 수정 가능
        question.validateOwner(userId);
        question.updateQuestion(title, content);
    }

    // todo questionInternalService.getQuestion(questionId); 단순화하기
    // 문의 삭제
    public void delete(Long questionId, Long userId) {
        Question question = questionInternalService.getQuestion(questionId);
        question.deleteQuestion(userId); // soft delete
    }
}
