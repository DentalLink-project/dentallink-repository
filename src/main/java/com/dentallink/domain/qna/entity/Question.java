package com.dentallink.domain.qna.entity;

import com.dentallink.common.entity.BaseEntity;
import com.dentallink.domain.qna.exception.QnaErrorCode;
import com.dentallink.domain.qna.exception.QnaException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "questions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "hospital_id", nullable = false)
    private Long hospitalId;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 질문 삭제 시 해당 질문에 달린 답변들도 함께 삭제
    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY)

    private List<Answer> answerList = new ArrayList<>();

    // 질문 생성 메서드
    public static Question of(Long userId, Long hospitalId, String title, String content) {
        Question question = new Question();
        question.userId = userId;
        question.hospitalId = hospitalId;
        question.title = title;
        question.content = content;
        return question;
    }

    // 질문 수정 메서드
    public void updateQuestion(String title, String content) {
        this.title = title;
        this.content = content;
    }

    /// 문의글 삭제 시 자신의 문의글 및 답변 일괄 삭제 메서드 - 본인 검증 포함
    public void deleteQuestion(Long userId) {
        validateOwner(userId);
        super.delete();     // 자기 자신 문의 삭제 처리 - soft delete
        answerList.forEach(Answer::forceDeleteAnswer);      // 해당 문의에 달린 답변들도 함께 강제 삭제 처리
        /**
         * 문의 작성자가 자신의 문의를 삭제할 때 사용하는 메서드.
         * - 작성자 본인 여부 검증
         * - soft delete 처리
         * - 관련 답변 모두 강제 삭제
         */
    }

    // 동일한 ID 검증 로직
    public void validateOwner(Long userId) {
        if (!this.getUserId().equals(userId)) {
            throw new QnaException(QnaErrorCode.QUESTION_ACCESS_DENIED);
        }
    }
}
