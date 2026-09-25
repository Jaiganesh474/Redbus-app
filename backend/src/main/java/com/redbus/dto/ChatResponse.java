package com.redbus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String sessionId;
    private String reply;
    private String toolExecuted;
    private Object toolData;
    private List<String> suggestedPrompts;
}
