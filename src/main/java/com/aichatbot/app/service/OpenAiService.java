package com.aichatbot.app.service;

import com.aichatbot.app.entity.Message;
import com.aichatbot.app.exception.OpenAiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class OpenAiService {

    private final RestClient restClient;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.model}")
    private String model;

    public OpenAiService(RestClient restClient) {
        this.restClient = restClient;
    }

    @SuppressWarnings("unchecked")
    public String getChatCompletion(List<Message> history, String prompt) {
        if (apiKey == null || apiKey.trim().isEmpty() || "your_openai_api_key_here".equals(apiKey)) {
            return generateSmartFallback(prompt);
        }

        try {
            List<Map<String, String>> messages = new ArrayList<>();
            
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", "You are Antigravity, a helpful AI assistant. Respond using clean Markdown. Keep formatting neat and professional.");
            messages.add(systemMsg);

            int start = Math.max(0, history.size() - 10);
            for (int i = start; i < history.size(); i++) {
                Message msg = history.get(i);
                Map<String, String> m = new HashMap<>();
                m.put("role", msg.getRole());
                m.put("content", msg.getContent());
                messages.add(m);
            }

            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);
            messages.add(userMsg);

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", model);
            payload.put("messages", messages);
            payload.put("temperature", 0.7);

            Map<String, Object> response = restClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, String> message = (Map<String, String>) choice.get("message");
                    if (message != null && message.containsKey("content")) {
                        return message.get("content");
                    }
                }
            }
            throw new OpenAiException("Invalid response format received from OpenAI API");

        } catch (Exception e) {
            throw new OpenAiException("Failed to fetch response from OpenAI API: " + e.getMessage(), e);
        }
    }

    public String generateSmartFallback(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return "Please ask a question, and I will do my best to explain it!";
        }

        List<String> subQuestions = parseSubQuestions(prompt);
        if (subQuestions.size() > 1) {
            StringBuilder combinedResponse = new StringBuilder();
            combinedResponse.append("### 🤖 Multi-Query Response\n\n");
            int index = 1;
            for (String subQ : subQuestions) {
                String subAnswer = resolveSingleQuery(subQ);
                combinedResponse.append("#### ").append(index).append(") ").append(subQ).append("\n\n");
                combinedResponse.append(subAnswer).append("\n\n---\n\n");
                index++;
            }
            if (combinedResponse.length() > 5) {
                combinedResponse.setLength(combinedResponse.length() - 7);
            }
            return combinedResponse.toString();
        }

        return resolveSingleQuery(prompt);
    }

    private List<String> parseSubQuestions(String prompt) {
        List<String> questions = new ArrayList<>();
        String[] parts = prompt.split("(?=\\b\\d+[\\)\\.]\\s+)|\\r?\\n");
        for (String part : parts) {
            String cleanPart = part.replaceAll("^\\d+[\\)\\.]\\s*", "").trim();
            cleanPart = cleanPart.replaceAll("(?i)\\b(these are my questions?|this is my question|as per the past|as per the current problem).*$", "").trim();
            if (!cleanPart.isEmpty() && cleanPart.length() > 2) {
                questions.add(cleanPart);
            }
        }
        if (questions.isEmpty()) {
            questions.add(prompt);
        }
        return questions;
    }

    private String resolveSingleQuery(String prompt) {
        String pollinationsResponse = fetchPollinationsAI(prompt);
        if (pollinationsResponse != null && !pollinationsResponse.trim().isEmpty()) {
            return pollinationsResponse;
        }

        return "### 💡 Fallback Notice\n\n" +
               "I'm currently unable to access the dynamic keyless AI service. Please verify your internet connection or check your OpenAI API key settings.";
    }

    private String fetchPollinationsAI(String query) {
        try {
            String url = "https://text.pollinations.ai/" + URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
            
            String response = restClient.get()
                    .uri(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/120.0.0.0")
                    .retrieve()
                    .body(String.class);
            
            if (response != null && !response.trim().isEmpty()) {
                return response;
            }
        } catch (Exception e) {
            System.err.println("Pollinations.ai fetch failed: " + e.getMessage());
        }
        return null;
    }
}
