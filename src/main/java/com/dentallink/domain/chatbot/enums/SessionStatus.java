package com.dentallink.domain.chatbot.enums;

public enum SessionStatus {
    ACTIVE("활성"),
    WAITING("대기중"),
    CLOSED("종료");

    private final String description;

    SessionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
