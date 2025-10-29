package com.dentallink.domain.chatbot.enums;

public enum MessageType {
    USER("사용자"),
    AI("AI"),
    CONSULTANT("상담원"),
    SYSTEM("시스템");

    private final String description;

    MessageType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
