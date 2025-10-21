package com.dentallink.domain.qna.enums;

public enum QuestionStatus {

    AWAITING("답변 대기 중"),    // 최초 문의 상태
    ANSWERED("답변 완료"),      // 답변 등록 시 변경
    REQUESTED("재문의");        // 사용자가 재문의 시 변경

    private final String description;

    QuestionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
