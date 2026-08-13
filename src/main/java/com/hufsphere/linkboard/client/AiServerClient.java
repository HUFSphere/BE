package com.hufsphere.linkboard.client;

import com.hufsphere.linkboard.client.dto.AskResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class AiServerClient {

    private final RestTemplate restTemplate;
    private final String aiServerUrl;

    public AiServerClient(RestTemplate restTemplate, @Value("${ai.server.url:http://localhost:8000}") String aiServerUrl) {
        this.restTemplate = restTemplate;
        this.aiServerUrl = aiServerUrl;
    }

    public void triggerSync(Long sourceId, String sourceRef) {
        String url = aiServerUrl + "/api/v1/sync";
        Map<String, Object> request = Map.of(
                "source_id", sourceId,
                "source_ref", sourceRef != null ? sourceRef : ""
        );
        try {
            restTemplate.postForEntity(url, request, String.class);
        } catch (Exception e) {
            System.err.println("AI 서버 동기화 요청 실패: " + e.getMessage());
        }
    }

    public AskResponse ask(String question, String lang) {
        String url = aiServerUrl + "/api/v1/qna";
        Map<String, Object> request = Map.of(
                "question", question,
                "lang", lang != null ? lang : "ko"
        );
        try {
            return restTemplate.postForObject(url, request, AskResponse.class);
        } catch (Exception e) {
            System.err.println("AI 서버 QnA 요청 실패: " + e.getMessage());
            return new AskResponse();
        }
    }
}