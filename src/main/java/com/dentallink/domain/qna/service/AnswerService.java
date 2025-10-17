package com.dentallink.domain.qna.service;

import com.dentallink.domain.qna.dto.response.AnswerResponseDto;
import com.dentallink.domain.qna.entity.Answer;
import com.dentallink.domain.qna.entity.Question;
import com.dentallink.domain.qna.repository.AnswerRepository;
import com.dentallink.domain.qna.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AnswerService {

    private final AnswerRepository answerRespository;
    private final QuestionRepository questionRespository;

    // 비즈니스 로직 작성 create
    public AnswerResponseDto create(Long questionId, Long responderId, String content) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("문의글을 찾을 수 없습니다."));
        Answer saved = answerRepository.save(Answer.of(question, responderId, content));
        return AnswerResponseDto.from(saved);
    }

    // 비즈니스 로직 작성 read
    @Transactional(readOnly = true)
    public Answer get(Long answerId) {
        return answerRepository.findById(answerId)
                .orElseThrow(() -> new IllegalArgumentException("답변글을 찾을 수 없습니다."));
    }

    // 비즈니스 로직 작성 update
    public void update(Long answerId, Long responderId, String content) {
        Answer answer = get(answerId);
        // 본인 답변만 수정 가능
        if (!answer.getResponderId().equals(responderId)) {
            throw new IllegalArgumentException("본인 답변글만 수정할 수 있습니다.");
        }
        answer.update(content);
    }

    // 비즈니스 로직 작성 delete
    public void delete(Long answerId, Long responderId) {
        Answer answer = get(answerId);
        // 본인 답변만 삭제 가능
        if (!answer.getResponderId().equals(responderId)) {
            throw new IllegalArgumentException("본인 답변글만 삭제할 수 있습니다.");
        }
        answer.delete(); // BaseEntity soft delete
    }
}
