package com.redbus.controller;

import com.redbus.dto.*;
import com.redbus.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/ai", "/api/v1/ai"})
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAiStatus() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "RedBus AI Assistant & NLP Engine",
                "endpoints", Map.of(
                        "chat", "POST /api/v1/ai/chat",
                        "parseQuery", "POST /api/v1/ai/parse-query",
                        "supportQuery", "POST /api/v1/ai/support-query",
                        "recommendations", "GET /api/v1/ai/recommendations"
                )
        ));
    }

    @GetMapping("/chat")
    public ResponseEntity<Map<String, Object>> getChatEndpointInfo() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "message", "RedBus AI Chat service is active. Send POST requests to this endpoint with a JSON body: {\"message\": \"...\", \"sessionId\": \"...\"}"
        ));
    }

    @PostMapping("/parse-query")
    public ResponseEntity<NlpParseResponse> parseQuery(@Valid @RequestBody NlpParseRequest request) {
        NlpParseResponse response = aiService.parseNaturalQuery(request.getQuery());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = aiService.chat(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/support-query")
    public ResponseEntity<SupportQueryResponse> supportQuery(@Valid @RequestBody SupportQueryRequest request) {
        SupportQueryResponse response = aiService.supportQuery(request.getQuestion());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recommendations")
    public ResponseEntity<RecommendationResponse> getRecommendations(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String destination
    ) {
        RecommendationResponse response = aiService.getRecommendations(userId, source, destination);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cities/search")
    public ResponseEntity<java.util.List<AiCityItemDto>> searchCities(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String exclude
    ) {
        java.util.List<AiCityItemDto> cities = aiService.searchCitiesWithAi(query, exclude);
        return ResponseEntity.ok(cities);
    }

    @GetMapping("/cities/points")
    public ResponseEntity<AiCityPointsDto> getCityPoints(@RequestParam String city) {
        AiCityPointsDto points = aiService.getAiCityPoints(city);
        return ResponseEntity.ok(points);
    }
}

