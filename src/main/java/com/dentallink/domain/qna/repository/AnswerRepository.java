package com.dentallink.domain.qna.repository;

import com.dentallink.domain.qna.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnswerRepository extends JpaRepository<Answer,Long> {

    // 삭제되지 않은 답변 조회
    Optional<Answer> findByIdAndDeletedFalse(Long id);

    // 1개의 문의에는 1개의 답변만 허용
    boolean existsByQuestionAndDeletedFalse(Long questionId);

    // 특정 문의글에 달린 삭제되지 않은 답변들 조회
    List<Answer> findByQuestion_IdAndDeletedFalse(Long questionId);

    /**
     * 특정 문의글에 달린 삭제되지 않은 답변들 조회에 대한 설명
     * - 메서드명: findByQuestion_IdAndDeletedFalse
     * - 기능: 특정 문의글(questionId)에 달린 삭제되지 않은 답변들(DeletedFalse)을 조회
     * - 파라미터: Long questionId - 조회할 문의글의 ID
     * - 반환값: List<Answer> - 해당 문의글에 달린 삭제되지 않은 답변들의 리스트
     *
     * Answer 엔티티의 question 필드(연관관계)의 id 값을 기준으로 답변들을 필터링하며,
     * deleted 필드가 false인 답변들만 조회합니다.
     *
     * SQL로 표현하면 다음과 같습니다:
     * SELECT *
     * FROM answers
     * WHERE question_id = :questionId
     *   AND deleted = false;
     * */
}
