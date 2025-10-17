package com.dentallink.domain.qna.repository;

import com.dentallink.domain.qna.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnswerRepository extends JpaRepository<Answer,Long> {

    // 삭제되지 않은 답변 조회
    Optional<Answer> findByIdAndDeletedFalse(Long id);

    // 특정 문의글에 달린 삭제되지 않은 답변들 조회
    List<Answer> findByQuestion_IdAndDeletedFalse(Long questionId);
}
