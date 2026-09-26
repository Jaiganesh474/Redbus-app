package com.redbus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BannerDto {
    private Long id;
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
    private LocalDateTime createdAt;
}
