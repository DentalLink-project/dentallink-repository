package com.dentallink.domain.chatbot.service;

import com.dentallink.common.config.GeminiConfig;
import com.dentallink.common.exception.GlobalException;
import com.dentallink.domain.chatbot.dto.GeminiFunction;
import com.dentallink.domain.chatbot.exception.ChatbotErrorCode;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Gemini API 통신 서비스
 * - Gemini API 호출 및 응답 처리
 * - Function Calling 지원
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiApiService {

    private final GeminiConfig geminiConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    private final Gson gson = new Gson();

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    /**
     * Gemini API 호출 (대화 + Function Calling)
     */
    public GeminiApiResponse generateContent(
            List<GeminiFunction.GeminiMessage> messages,
            List<GeminiFunction.FunctionDeclaration> functions) {

        String url = String.format(GEMINI_API_URL, geminiConfig.getModelName(), geminiConfig.getApiKey());

        Map<String, Object> requestBody = buildRequestBody(messages, functions);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            return parseGeminiResponse(response.getBody());

        } catch (HttpClientErrorException e) {
            log.error("Gemini API 호출 실패: {}", e.getMessage());

            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                throw new GlobalException(ChatbotErrorCode.RATE_LIMIT_EXCEEDED);
            }

            throw new GlobalException(ChatbotErrorCode.GEMINI_API_ERROR);
        } catch (Exception e) {
            log.error("Gemini API 처리 중 오류 발생", e);
            throw new GlobalException(ChatbotErrorCode.GEMINI_API_ERROR);
        }
    }

    /**
     * 요청 바디 구성
     */
    private Map<String, Object> buildRequestBody(
            List<GeminiFunction.GeminiMessage> messages,
            List<GeminiFunction.FunctionDeclaration> functions) {

        Map<String, Object> requestBody = new HashMap<>();

        // Contents 구성
        List<Map<String, Object>> contents = new ArrayList<>();
        for (GeminiFunction.GeminiMessage message : messages) {
            Map<String, Object> content = new HashMap<>();
            content.put("role", message.role());

            List<Map<String, Object>> parts = new ArrayList<>();

            // 텍스트 파트
            if (message.content() != null && !message.content().isEmpty()) {
                Map<String, Object> textPart = new HashMap<>();
                textPart.put("text", message.content());
                parts.add(textPart);
            }

            // Function Call 파트
            if (message.functionCalls() != null && !message.functionCalls().isEmpty()) {
                for (GeminiFunction.FunctionCall fc : message.functionCalls()) {
                    Map<String, Object> fcPart = new HashMap<>();
                    Map<String, Object> fcData = new HashMap<>();
                    fcData.put("name", fc.name());
                    fcData.put("args", fc.arguments());
                    fcPart.put("functionCall", fcData);
                    parts.add(fcPart);
                }
            }

            // Function Response 파트
            if (message.functionResponses() != null && !message.functionResponses().isEmpty()) {
                for (GeminiFunction.FunctionResponse fr : message.functionResponses()) {
                    Map<String, Object> frPart = new HashMap<>();
                    Map<String, Object> frData = new HashMap<>();
                    frData.put("name", fr.name());
                    frData.put("response", fr.response());
                    frPart.put("functionResponse", frData);
                    parts.add(frPart);
                }
            }

            content.put("parts", parts);
            contents.add(content);
        }
        requestBody.put("contents", contents);

        // Tools (Functions) 구성
        if (functions != null && !functions.isEmpty()) {
            List<Map<String, Object>> toolsList = new ArrayList<>();
            Map<String, Object> tool = new HashMap<>();

            List<Map<String, Object>> functionDeclarations = new ArrayList<>();
            for (GeminiFunction.FunctionDeclaration func : functions) {
                Map<String, Object> funcDecl = new HashMap<>();
                funcDecl.put("name", func.name());
                funcDecl.put("description", func.description());
                funcDecl.put("parameters", func.parameters());
                functionDeclarations.add(funcDecl);
            }

            tool.put("functionDeclarations", functionDeclarations);
            toolsList.add(tool);
            requestBody.put("tools", toolsList);
        }

        // Generation Config
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", geminiConfig.getTemperature());
        generationConfig.put("topK", geminiConfig.getTopK());
        generationConfig.put("topP", geminiConfig.getTopP());
        generationConfig.put("maxOutputTokens", geminiConfig.getMaxTokens());
        requestBody.put("generationConfig", generationConfig);

        return requestBody;
    }

    /**
     * Gemini 응답 파싱
     */
    private GeminiApiResponse parseGeminiResponse(String responseBody) {
        JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

        if (!jsonResponse.has("candidates") || jsonResponse.getAsJsonArray("candidates").isEmpty()) {
            throw new GlobalException(ChatbotErrorCode.GEMINI_API_ERROR);
        }

        JsonObject candidate = jsonResponse.getAsJsonArray("candidates").get(0).getAsJsonObject();
        JsonObject content = candidate.getAsJsonObject("content");

        String text = null;
        List<GeminiFunction.FunctionCall> functionCalls = new ArrayList<>();

        if (content.has("parts")) {
            for (var part : content.getAsJsonArray("parts")) {
                JsonObject partObj = part.getAsJsonObject();

                // 텍스트 응답
                if (partObj.has("text")) {
                    text = partObj.get("text").getAsString();
                }

                // Function Call
                if (partObj.has("functionCall")) {
                    JsonObject fc = partObj.getAsJsonObject("functionCall");
                    String name = fc.get("name").getAsString();
                    Map<String, Object> args = gson.fromJson(
                            fc.get("args"),
                            Map.class
                    );
                    functionCalls.add(GeminiFunction.FunctionCall.builder()
                            .name(name)
                            .arguments(args)
                            .build());
                }
            }
        }

        return new GeminiApiResponse(text, functionCalls);
    }

    /**
     * Gemini API 응답 래퍼
     */
    public record GeminiApiResponse(
            String text,
            List<GeminiFunction.FunctionCall> functionCalls
    ) {
        public boolean hasFunctionCalls() {
            return functionCalls != null && !functionCalls.isEmpty();
        }
    }
}