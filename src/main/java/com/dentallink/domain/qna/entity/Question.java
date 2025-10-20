package com.dentallink.domain.qna.entity;

import com.dentallink.common.entity.BaseEntity;
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

    // 질문 삭제 메서드 (soft delete)
    public void deleteQuestion() {
        this.delete();
        if (answerList != null) {
            answerList.forEach(Answer::deleteAnswer);
        }
    }
}
