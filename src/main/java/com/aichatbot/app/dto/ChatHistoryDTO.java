package com.aichatbot.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryDTO {
    private Long id;
    private String title;
    private LocalDateTime createdAt;
}
