package com.dentallink.domain.qna.service;

import com.dentallink.domain.qna.entity.Answer;
import com.dentallink.domain.qna.entity.Question;
import com.dentallink.domain.qna.exception.QnaErrorCode;
import com.dentallink.domain.qna.exception.QnaException;
import com.dentallink.domain.qna.repository.AnswerRepository;
import com.dentallink.domain.qna.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnswerInternalService {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;

    public Answer getAnswer(Long answerId) {
        return answerRepository.findByIdAndDeletedFalse(answerId)
                .orElseThrow(() -> new QnaException(QnaErrorCode.ANSWER_NOT_FOUND));
    }

    public Question getQuestion(Long questionId) {
        return questionRepository.findByIdAndDeletedFalse(questionId)
                .orElseThrow(() -> new QnaException(QnaErrorCode.QUESTION_NOT_FOUND));
    }

    @Transactional
    public Answer save(Answer answer) {
        return answerRepository.save(answer);
    }

    @Transactional
    public void delete(Answer answer) {
        answerRepository.delete(answer);
    }

    // 특정 문의에 대해 활성화된 답변이 존재하는지 확인
    public boolean existsActiveAnswer(Long questionId) {
        return answerRepository.existsByQuestionIdAndDeletedFalse(questionId);
    }
}
