package com.redbus.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateAiBannerRequest {
    @NotBlank(message = "Prompt is required for AI banner generation")
    private String prompt;
    private String targetRoute;
    private Integer targetDiscount;
}
