package com.aichatbot.app.dto;

import lombok.Data;

@Data
public class ChatRequestDTO {
    private String content;
    private Long chatId; // null if creating a new conversation
    private Long userId; // null if using the seeded default user
}
