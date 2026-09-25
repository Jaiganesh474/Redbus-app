package com.redbus.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String sessionId;

    @NotBlank(message = "message cannot be empty")
    private String message;

    private Long userId;
    private String sourceCity;
    private String destinationCity;
    private String travelDate;
}
