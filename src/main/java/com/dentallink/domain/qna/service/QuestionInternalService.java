package com.dentallink.domain.qna.service;

import com.dentallink.domain.qna.entity.Question;
import com.dentallink.domain.qna.exception.QnaErrorCode;
import com.dentallink.domain.qna.exception.QnaException;
import com.dentallink.domain.qna.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestionInternalService {

    private final QuestionRepository questionRepository;

    // 단일 질문 조회
    public Question getQuestion(Long questionId) {
        return questionRepository.findByIdAndDeletedFalse(questionId)
                .orElseThrow(() -> new QnaException(QnaErrorCode.QUESTION_NOT_FOUND));
    }

    // 여러 질문 조회
    public List<Question> getQuestions() {
        return questionRepository.findAllByDeletedFalseOrderByCreatedAtDesc();
    }

    @Transactional
    public Question save(Question question) {
        return questionRepository.save(question);
    }

    @Transactional
    public void delete(Question question) {
        questionRepository.delete(question);
    }

    // 제목으로 질문 존재 여부 확인
    public boolean existsByTitle(String title) {
        return questionRepository.existsByTitleAndDeletedFalse(title);
    }
}
