package com.dentallink.domain.chatbot.enums;

public enum ChatMode {

    AI("AI 챗봇"),
    CONSULTANT("상담원");

    private final String description;

    ChatMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
