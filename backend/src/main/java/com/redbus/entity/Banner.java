package com.redbus.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "banners")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String tag;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String subtitle;

    @Column(name = "cta_text", nullable = false, length = 100)
    @Builder.Default
    private String ctaText = "Explore Deals";

    @Column(name = "cta_link", nullable = false)
    @Builder.Default
    private String ctaLink = "/search";

    @Column(name = "bg_gradient", length = 150)
    @Builder.Default
    private String bgGradient = "from-slate-950 via-red-950/80 to-slate-900";

    @Column(name = "badge_color", length = 150)
    @Builder.Default
    private String badgeColor = "bg-red-500/20 border-red-500/30 text-red-300";

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(length = 100)
    private String accent;

    @Column(name = "route_info", length = 150)
    private String routeInfo;

    @Column(name = "promo_code", length = 50)
    private String promoCode;

    @Column(name = "discount_percentage")
    @Builder.Default
    private Integer discountPercentage = 0;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 1;

    @Column(name = "is_ai_generated")
    @Builder.Default
    private Boolean isAiGenerated = false;

    @Column(name = "prompt_used", length = 1000)
    private String promptUsed;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

