package com.dentallink.domain.qna.entity;

import com.dentallink.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
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

    @Column(length = 255, nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 질문 삭제 시 해당 질문에 달린 답변들도 함께 삭제
    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<Answer> answerList = List.of();

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

    // 질문 소프트 삭제 메서드
    public void deleteQuestion() {
        this.delete(); // BaseEntity의 soft_delete 메서드 호출
        this.deletedAt = LocalDateTime.now();
        // 필요 시 답변도 삭제 처리
        if (answerList != null) {
            for (Answer answer : answerList) {
                answer.delete(); // Answer 엔티티의 soft_delete 메서드 호출
            }
        }
    }
}
