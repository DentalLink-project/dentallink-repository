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
    public QuestionResponseDto create(Long userId, Long hospitalId, String title, String content) {
        Question saved = questionRepository.save(Question.of(userId, hospitalId, title, content));
        return QuestionResponseDto.from(saved);
    }

    // 비즈니스 로직 작성 read
    @Transactional(readOnly = true)
    public Question get(Long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("문의글을 찾을 수 없습니다."));
    }

    // 답변과 함께 문의글 조회
    @Transactional(readOnly = true)
    public Question getWithAnswers(Long id) {
        return questionRepository.findWithAnswersById(id)
                .orElseThrow(() -> new IllegalArgumentException("문의글을 찾을 수 없습니다."));
    }

    // 비즈니스 로직 작성 update
    public void update(Long questionId, Long userId, String title, String content) {
        Question question = get(questionId);
        // 본인 질문만 수정 가능
        if (!question.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인 문의글만 수정할 수 있습니다.");
        }
        question.update(title, content);
    }

    // 비즈니스 로직 작성 delete
    public void delete(Long questionId, Long userId) {
        Question question = get(questionId);
        if (!question.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인 문의글만 삭제할 수 있습니다.");
        }
        question.delete(); // soft delete
    }
}
