package com.dentallink.qna.entity;

import com.dentallink.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "answers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Answer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    // 답변을 작성한 사용자 ID (userId와 동일)
    @Column(name = "responder_id", nullable = false)
    private Long responderId;

    @Column(length = 255, nullable = false, columnDefinition = "TEXT")
    private String content;

    private Long questionId;
    private Long userId;

    @Column(name = "deleted_at")
    private java.time.LocalDateTime deletedAt;

    // 생성 팩토리 메서드
    public static Answer createAnswer(Question question, Long responderId, String content) {
        Answer answer = new Answer();
        answer.question = question;
        answer.responderId = responderId;
        answer.content = content;
        return answer;
    }

    // 답변 수정 메서드
    public void updateAnswer(String content) {
        this.content = content;
    }

    // 답변 소프트 삭제 메서드
    public void deleteAnswer() {
        this.delete(); // BaseEntity의 soft_delete 메서드 호출
        this.deletedAt = LocalDateTime.now();
    }
}
