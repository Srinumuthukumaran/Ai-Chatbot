package com.aichatbot.app.controller;

import com.aichatbot.app.dto.ChatHistoryDTO;
import com.aichatbot.app.dto.ChatRequestDTO;
import com.aichatbot.app.dto.ChatResponseDTO;
import com.aichatbot.app.dto.SaveAiResponseRequestDTO;
import com.aichatbot.app.dto.MessageDTO;
import com.aichatbot.app.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*") // Enable cross-origin requests for flexible local testing
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/send")
    public ResponseEntity<ChatResponseDTO> sendMessage(@RequestBody ChatRequestDTO request) {
        ChatResponseDTO response = chatService.sendMessage(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/save-ai-response")
    public ResponseEntity<Void> saveAiResponse(@RequestBody SaveAiResponseRequestDTO request) {
        chatService.saveAiResponse(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/generate-fallback")
    public ResponseEntity<ChatResponseDTO> generateFallback(@RequestBody ChatRequestDTO request) {
        ChatResponseDTO response = chatService.generateFallback(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<ChatHistoryDTO>> getChatHistory(@RequestParam(required = false) Long userId) {
        List<ChatHistoryDTO> history = chatService.getChatHistory(userId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{id}")
    public ResponseEntity<List<MessageDTO>> getChatById(@PathVariable Long id) {
        List<MessageDTO> messages = chatService.getChatMessages(id);
        return ResponseEntity.ok(messages);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChat(@PathVariable Long id) {
        chatService.deleteChat(id);
        return ResponseEntity.noContent().build();
    }
}
