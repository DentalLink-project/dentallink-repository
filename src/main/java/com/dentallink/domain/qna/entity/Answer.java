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
        question.getAnswerList().add(answer); // Answer of() 생성자에서 양방향 연관관계 설정
        return answer;
    }

    // 답변 수정 메서드
    public void updateAnswer(String content) {
        this.content = content;
    }

    /// 일반 사용자 삭제 메서드 - Only 본인 답변
    public void deleteAnswer(Long responderId) {
        validateResponder(responderId);
        super.delete(); // soft delete - BaseEntity의 delete 메서드 호출
    }
    /**
     * 설명
     * 답변 작성자 (responderId) 본인이 직접 삭제할 때 사용하는 메서드
     * "내가 작성한 답변을 내가 삭제할 수 있다."는 비즈니스 규칙을 구현
     * 검증 로직은 validateResponder 메서드에서 수행
     * 삭제 주체의 권한 검증을 우회하는 로직이 필요할 경우 이 메서드를 수정하거나 별도의 메서드를 추가해야 함
     * */

    /// 문의글 삭제 시 답변 일괄 삭제 메서드 - 검증 우회용 메서드
    // 질문 삭제 시 사용 또는 관리자 권한 등 특별한 경우에 사용
    protected void forceDeleteAnswer() {
        super.delete(); // soft delete - BaseEntity의 delete 메서드 호출
    }
    // protected로 설정하여 Answer 외부에서 직접 호출하지 못하게 함

    // 동일한 ID 검증 로직
    public void validateResponder(Long responderId) {
        if (!this.getResponderId().equals(responderId)) {
            throw new QnaException(QnaErrorCode.ANSWER_ACCESS_DENIED);
        }
    }
}
