package com.dentallink.domain.qna.service;

import com.dentallink.domain.qna.dto.response.QuestionResponseDto;
import com.dentallink.domain.qna.entity.Question;
import com.dentallink.domain.qna.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class QuestionService {

    private final QuestionRepository questionRepository;

    // 비즈니스 로직 작성 creeate
    public QuestionResponseDto.QuestionResponse create(Long userId, Long hospitalId, String title, String content) {
        Question saved = questionRepository.save(Question.of(userId, hospitalId, title, content));
        return QuestionResponseDto.QuestionResponse.from(saved);
    }

    // 비즈니스 로직 작성 read
    @Transactional(readOnly = true)
    public Question get(Long questionId) {
        /**
         * todo: questionRepository.findById()를 사용하면 논리적으로 삭제된(soft-deleted) 질문도 조회될 수 있습니다.
         * deleted = false인 질문만 조회하도록 findByIdAndDeletedFalse() 메소드를 사용해야 합니다.
         * 이 get 메소드는 update와 delete에서도 사용되므로 수정이 필요합니다.
         *
         * return questionRepository.findByIdAndDeletedFalse(questionId)
         *                 .orElseThrow(() -> new IllegalArgumentException("문의글을 찾을 수 없습니다."));
         * */
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("문의글을 찾을 수 없습니다."));
    }

    // 답변과 함께 문의글 조회
    @Transactional(readOnly = true)
    public Question getWithAnswers(Long id) {
        return questionRepository.findActiveWithAnswersById(id)
                .orElseThrow(() -> new IllegalArgumentException("문의글을 찾을 수 없습니다."));
    }

    // 비즈니스 로직 작성 update
    public void update(Long questionId, Long userId, String title, String content) {
        Question question = get(questionId);
        // 본인 질문만 수정 가능
        if (!question.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인 문의글만 수정할 수 있습니다.");
        }
        question.updateQuestion(title, content);
    }

    // 비즈니스 로직 작성 delete
    public void delete(Long questionId, Long userId) {
        Question question = get(questionId);
        if (!question.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인 문의글만 삭제할 수 있습니다.");
        }
        question.deleteQuestion(); // soft delete
        /**
         * todo: delete 메소드에서 get(questionId)를 호출하여 질문을 조회하고 있습니다. 이 get 메소드는 answerList를 함께 가져오지 않습니다.
         * 따라서 question.deleteQuestion() 내부에서 answerList에 접근할 때 지연 로딩이 발생하여 추가적인 쿼리가 실행됩니다 (N+1 문제).
         * 삭제 시에는 질문과 답변 목록을 함께 조회하는 것이 효율적입니다. getWithAnswers 메소드를 재사용하는 것을 고려해보세요.
         * */
    }
}
