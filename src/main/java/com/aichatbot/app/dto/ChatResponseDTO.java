package com.aichatbot.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDTO {
    private String content; // AI message content
    private Long chatId;
    private String chatTitle;
    private LocalDateTime timestamp;
}
