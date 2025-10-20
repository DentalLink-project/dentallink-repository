package com.dentallink.domain.qna.repository;

import com.dentallink.domain.qna.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    // 삭제되지 않은 문의글 조회
    Optional<Question> findByIdAndDeletedFalse(Long id);

    // 제목(title)으로 단건 조회
    Optional<Question> findByTitle(String title);

    // 제목(title)과 내용(content)으로 단건 조회
    Optional<Question> findByTitleAndContent(String title, String content);

    // 제목(title)에 특정 문자열이 포함된 문의글 조회
    Optional<Question> findByTitleContaining(String keyword);

    // 페이징 전체 조회
    Page<Question> findAll(Pageable pageable);

    // 삭제되지 않은 문의글과 답변 함께 조회
    @EntityGraph(attributePaths = "answerList")

    // 삭제되지 않은 문의글과 해당 문의글에 달린 삭제되지 않은 답변들까지 함께 조회 (JPQL을 사용)
    // answerlist is null or answerlist.deleted = false 사용 이유 : 답변이 없는 문의글도 조회하기 위함
    // distinct 사용 이유 : left join fetch로 인해 중복된 문의글이 조회되는 것을 방지하기 위함
    @Query("""
        select distinct question
        from Question question
        left join fetch question.answerList answerlist
        where question.id = :id
          and question.deleted = false
          and (answerlist is null or answerlist.deleted = false)
    """)

    // 삭제되지 않은 문의글과 해당 문의글에 달린 삭제되지 않은 답변들까지 함께 조회
    Optional<Question> findActiveWithAnswersById(
            @Param("id") Long id
    );

    // 키워드를 기반으로 검색 (제목과 내용 모두 검색 대상 포함)
    @Query("""
        select question
        from Question question
        where question.deleted = false
          and (question.title like %:keyword% or question.content like %:keyword%)
    """)
    Page<Question> findByKeyword(
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
