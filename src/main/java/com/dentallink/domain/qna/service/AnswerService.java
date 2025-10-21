package com.dentallink.domain.qna.service;

import com.dentallink.domain.qna.dto.response.AnswerResponseDto;
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
@Transactional
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;

    // 비즈니스 로직 작성 create
    public AnswerResponseDto.AnswerResponse create(Long questionId, Long responderId, String content) {
        Question question = questionRepository.findByIdAndDeletedFalse(questionId)
                         .orElseThrow(() -> new QnaException(QnaErrorCode.QUESTION_NOT_FOUND));

        // 하나의 문의에는 하나의 답변만 작성 가능
        // ver1
        if (answerRepository.existsByQuestionAndDeletedFalse(questionId)) {
            throw new QnaException(QnaErrorCode.ANSWER_DUPLICATE);
        }

        // ver2
        // boolean existsAnswer = answerRepository.findByQuestionAndDeletedFalse(questionId);
        // if (existsAnswer) {
        //     throw new QnaException(QnaErrorCode.ANSWER_DUPLICATE);
        // }

        // 답변 생성
        Answer saved = answerRepository.save(Answer.of(question, responderId, content));

        // 문의 상태 변경 : Awaiting(기본값, 답변 대기 중) -> Answered (답변완료)
        question.answeredMark();
        return AnswerResponseDto.AnswerResponse.from(saved);
    }

    // 비즈니스 로직 작성 read
    @Transactional(readOnly = true)
    public Answer get(Long answerId) {
        return answerRepository.findByIdAndDeletedFalse(answerId)
                .orElseThrow(() -> new QnaException(QnaErrorCode.ANSWER_NOT_FOUND));
    }

    // 비즈니스 로직 작성 update
    public void update(Long answerId, Long responderId, String content) {
        Answer answer = get(answerId);
        // 본인 답변만 수정 가능
        answer.validateResponder(responderId);
        answer.updateAnswer(content);
    }

    // 비즈니스 로직 작성 delete
    public void delete(Long answerId, Long responderId) {
        Answer answer = get(answerId);
        answer.deleteAnswer(responderId); // soft delete

        // 답변 삭제 시 문의 상태 변경 : Answered -> Awaiting
        Question question = answer.getQuestion();
        question.awaitingMark();
    }
}
