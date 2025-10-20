package com.dentallink.domain.qna.entity;

import com.dentallink.common.entity.BaseEntity;
import com.dentallink.domain.qna.exception.QnaErrorCode;
import com.dentallink.domain.qna.exception.QnaException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "answers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Answer extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    // 답변을 작성한 사용자 ID (userId와 동일)
    @Column(name = "responder_id", nullable = false)
    private Long responderId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 생성 팩토리 메서드
    public static Answer of(Question question, Long responderId, String content) {
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

    // 답변 삭제 메서드 (soft delete) + 검증 로직
    // todo 검증 로직 추가
    public void deleteAnswer() {
        this.delete();
    }

    // 동일한 ID 검증 로직
    public void validateResponder(Long responderId) {
        if (!this.getResponderId().equals(responderId)) {
            throw new QnaException(QnaErrorCode.ANSWER_ACCESS_DENIED);
        }
    }
}
