package com.aichatbot.app.service;

import com.aichatbot.app.dto.ChatHistoryDTO;
import com.aichatbot.app.dto.ChatRequestDTO;
import com.aichatbot.app.dto.ChatResponseDTO;
import com.aichatbot.app.dto.MessageDTO;
import com.aichatbot.app.dto.SaveAiResponseRequestDTO;
import java.util.List;

public interface ChatService {
    ChatResponseDTO sendMessage(ChatRequestDTO request);
    List<ChatHistoryDTO> getChatHistory(Long userId);
    List<MessageDTO> getChatMessages(Long chatId);
    void deleteChat(Long chatId);
    void saveAiResponse(SaveAiResponseRequestDTO request);
    ChatResponseDTO generateFallback(ChatRequestDTO request);
}
