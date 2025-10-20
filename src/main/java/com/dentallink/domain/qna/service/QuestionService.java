package com.dentallink.domain.qna.service;

import com.dentallink.domain.qna.dto.response.QuestionResponseDto;
import com.dentallink.domain.qna.entity.Question;
import com.dentallink.domain.qna.exception.QnaErrorCode;
import com.dentallink.domain.qna.exception.QnaException;
import com.dentallink.domain.qna.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class QuestionService {

    private final QuestionRepository questionRepository;

    // 비즈니스 로직 작성 create
    public QuestionResponseDto.QuestionResponse create(Long userId, Long hospitalId, String title, String content) {
        Question saved = questionRepository.save(Question.of(userId, hospitalId, title, content));
        return QuestionResponseDto.QuestionResponse.from(saved);
    }

    // 비즈니스 로직 작성 read
    @Transactional(readOnly = true)
    public Question get(Long questionId) {
        return questionRepository.findByIdAndDeletedFalse(questionId)
                .orElseThrow(() -> new QnaException(QnaErrorCode.QUESTION_NOT_FOUND));
    }

    // 답변과 함께 문의글 조회
    @Transactional(readOnly = true)
    public Question getWithAnswers(Long id) {
        return questionRepository.findActiveWithAnswersById(id)
                .orElseThrow(() -> new QnaException(QnaErrorCode.QUESTION_NOT_FOUND));
    }

    // 비즈니스 로직 작성 update
    public void update(Long questionId, Long userId, String title, String content) {
        Question question = get(questionId);
        // 본인 질문만 수정 가능
        question.validateOwner(userId);
        question.updateQuestion(title, content);
    }

    // 비즈니스 로직 작성 delete
    public void delete(Long questionId, Long userId) {
        Question question = getWithAnswers(questionId);
        question.validateOwner(userId);
        question.deleteQuestion(userId); // soft delete
    }
}
