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
        if (answerRepository.existsByQuestionIdAndDeletedFalse(questionId)) {
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

        // todo answer.getQuestion() 호출은 Question 엔티티에 대한 LAZY 로딩으로 인해 별도의 SELECT 쿼리를 유발할 수 있습니다.
        //  이는 N+1 문제로 이어질 수 있는 잠재적인 성능 저하 지점입니다.
        //  get(answerId) 메서드에서 Answer를 조회할 때 JOIN FETCH나 @EntityGraph를 사용하여 연관된 Question 엔티티를 함께 가져오도록 수정하는 것을 고려해 보세요.
        //  이렇게 하면 불필요한 추가 쿼리를 방지할 수 있습니다.
        // 답변 삭제 시 문의 상태 변경 : Answered -> Awaiting
        Question question = answer.getQuestion();
        question.awaitingMark();
    }
}
