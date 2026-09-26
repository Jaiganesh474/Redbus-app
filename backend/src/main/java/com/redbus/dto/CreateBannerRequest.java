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
public class CreateBannerRequest {
    @NotBlank(message = "Title is required")
    private String title;
    private String subtitle;
    private String tag;
    private String promoCode;
    private Integer discountPercentage;
    private String bgGradient;
    private String badgeColor;
    private String routeInfo;
    private String imageUrl;
    private String ctaText;
    private String ctaLink;
    private Boolean active;
    private Integer sortOrder;
    private Boolean isAiGenerated;
    private String promptUsed;
}
