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
    // todo: List.of() 제거 검토
    /**
     * answerList를 List.of()로 초기화하면 불변 리스트가 생성됩니다.
     * JPA가 엔티티를 로드할 때는 이 컬렉션을 자체 구현으로 교체하지만,
     * 새로 생성된 Question 객체의 answerList에 요소를 추가하려고 하면 UnsupportedOperationException이 발생할 수 있습니다.
     * 안전하게 가변 리스트인 new ArrayList<>()로 초기화하는 것이 좋습니다.
     * */
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
    // 질문 삭제 메서드 (soft delete) + 검증 로직
    // todo 검증 로직 추가
    public void deleteQuestion(Long userId) {
        validateOwner(userId);

        // 자기 자신 질문 삭제 처리 - soft delete - BaseEntity의 delete 메서드 호출
        super.delete();

        // 해당 질문에 달린 답변들도 함께 강제 삭제 처리
        if (answerList != null) {
            answerList.forEach(Answer::forceDeleteAnswer);
        }
        /**
         * 설명
         * 질문 작성자 (userId) 본인이 직접 삭제할 때 사용하는 메서드
         * "내가 작성한 질문을 내가 삭제할 수 있다."는 비즈니스 규칙을 구현
         * 검증 로직은 validateOwner 메서드에서 수행
         * 삭제 주체의 권한 검증을 우회하는 로직이 필요할 경우 이 메서드를 수정하거나 별도의 메서드를 추가해야 함
         *
         * 문제
         * 현재 deleteAnswer 메서드는 답변 작성자 (responderId) 본인이 삭제할 때만 호출할 수 있도록 되어 있음
         * 질문 작성자가 자신의 질문을 삭제할 때 해당 질문에 달린 답변들도 함께 삭제하려고 하면
         * 답변 작성자와 질문 작성자가 다를 경우 권한 검증에서 예외가 발생함
         *
         * 해결 방안
         * deleteAnswer 메서드에 별도의 삭제 주체 검증 우회 로직을 추가하거나,
         * Question 엔티티에서 답변 삭제를 직접 처리하는 로직을 구현해야 함
         * */
    }

    // 동일한 ID 검증 로직
    public void validateOwner(Long userId) {
        if (!this.getUserId().equals(userId)) {
            throw new QnaException(QnaErrorCode.QUESTION_ACCESS_DENIED);
        }
    }
}
