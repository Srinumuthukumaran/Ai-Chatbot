package com.aichatbot.app.service;

import com.aichatbot.app.dto.ChatHistoryDTO;
import com.aichatbot.app.dto.ChatRequestDTO;
import com.aichatbot.app.dto.ChatResponseDTO;
import com.aichatbot.app.dto.MessageDTO;
import com.aichatbot.app.dto.SaveAiResponseRequestDTO;
import com.aichatbot.app.entity.Chat;
import com.aichatbot.app.entity.Message;
import com.aichatbot.app.entity.User;
import com.aichatbot.app.exception.ResourceNotFoundException;
import com.aichatbot.app.repository.ChatRepository;
import com.aichatbot.app.repository.MessageRepository;
import com.aichatbot.app.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChatServiceImpl implements ChatService {

    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final OpenAiService openAiService;

    public ChatServiceImpl(UserRepository userRepository,
                           ChatRepository chatRepository,
                           MessageRepository messageRepository,
                           OpenAiService openAiService) {
        this.userRepository = userRepository;
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
        this.openAiService = openAiService;
    }

    @Override
    public ChatResponseDTO sendMessage(ChatRequestDTO request) {
        User user;
        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));
        } else {
            user = userRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("No users found in database. Please verify seeding."));
        }

        Chat chat;

        if (request.getChatId() != null) {
            chat = chatRepository.findById(request.getChatId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chat thread not found with id: " + request.getChatId()));
        } else {
            chat = new Chat();
            String rawContent = request.getContent();
            String title = rawContent.length() > 30 ? rawContent.substring(0, 27) + "..." : rawContent;
            chat.setTitle(title);
            chat.setUser(user);
            chat.setCreatedAt(LocalDateTime.now());
            chat = chatRepository.save(chat);
        }

        Message userMessage = new Message();
        userMessage.setChat(chat);
        userMessage.setRole("user");
        userMessage.setContent(request.getContent());
        userMessage.setTimestamp(LocalDateTime.now());
        messageRepository.save(userMessage);

        List<Message> history = messageRepository.findByChatIdOrderByTimestampAsc(chat.getId());

        String aiReply = openAiService.getChatCompletion(history, request.getContent());

        Message aiMessage = new Message();
        aiMessage.setChat(chat);
        aiMessage.setRole("assistant");
        aiMessage.setContent(aiReply);
        aiMessage.setTimestamp(LocalDateTime.now());
        messageRepository.save(aiMessage);

        return new ChatResponseDTO(
                aiReply,
                chat.getId(),
                chat.getTitle(),
                aiMessage.getTimestamp()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatHistoryDTO> getChatHistory(Long userId) {
        User user;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        } else {
            user = userRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("No default user available. Database is empty."));
        }

        return chatRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(chat -> new ChatHistoryDTO(chat.getId(), chat.getTitle(), chat.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageDTO> getChatMessages(Long chatId) {
        if (!chatRepository.existsById(chatId)) {
            throw new ResourceNotFoundException("Chat thread not found with id: " + chatId);
        }

        return messageRepository.findByChatIdOrderByTimestampAsc(chatId).stream()
                .map(msg -> new MessageDTO(msg.getId(), msg.getRole(), msg.getContent(), msg.getTimestamp()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteChat(Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat thread not found with id: " + chatId));
        chatRepository.delete(chat);
    }

    @Override
    @Transactional
    public void saveAiResponse(SaveAiResponseRequestDTO request) {
        Chat chat = chatRepository.findById(request.getChatId())
                .orElseThrow(() -> new ResourceNotFoundException("Chat thread not found with id: " + request.getChatId()));
        
        Message aiMessage = new Message();
        aiMessage.setChat(chat);
        aiMessage.setRole("assistant");
        aiMessage.setContent(request.getContent());
        aiMessage.setTimestamp(LocalDateTime.now());
        messageRepository.save(aiMessage);
    }

    @Override
    @Transactional
    public ChatResponseDTO generateFallback(ChatRequestDTO request) {
        Chat chat = chatRepository.findById(request.getChatId())
                .orElseThrow(() -> new ResourceNotFoundException("Chat thread not found with id: " + request.getChatId()));
        
        String fallbackAnswer = openAiService.generateSmartFallback(request.getContent());
        
        Message aiMessage = new Message();
        aiMessage.setChat(chat);
        aiMessage.setRole("assistant");
        aiMessage.setContent(fallbackAnswer);
        aiMessage.setTimestamp(LocalDateTime.now());
        messageRepository.save(aiMessage);
        
        return new ChatResponseDTO(
                fallbackAnswer,
                chat.getId(),
                chat.getTitle(),
                aiMessage.getTimestamp()
        );
    }
}
