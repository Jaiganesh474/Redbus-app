package com.redbus.service;

import com.redbus.dto.BannerDto;
import com.redbus.dto.CreateBannerRequest;
import com.redbus.dto.GenerateAiBannerRequest;
import com.redbus.entity.Banner;
import com.redbus.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BannerService {

    private final BannerRepository bannerRepository;
    private final Random random = new Random();

    @Transactional(readOnly = true)
    public List<BannerDto> getActiveBanners() {
        return bannerRepository.findByActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BannerDto> getAllBannersForAdmin() {
        return bannerRepository.findAllByOrderBySortOrderAsc()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BannerDto createBanner(CreateBannerRequest request) {
        Banner banner = Banner.builder()
                .title(request.getTitle())
                .subtitle(request.getSubtitle())
                .tag(request.getTag() != null ? request.getTag() : "EXCLUSIVE OFFER")
                .promoCode(request.getPromoCode())
                .discountPercentage(request.getDiscountPercentage() != null ? request.getDiscountPercentage() : 0)
                .bgGradient(request.getBgGradient() != null ? request.getBgGradient() : "from-slate-950 via-red-950/80 to-slate-900")
                .badgeColor(request.getBadgeColor() != null ? request.getBadgeColor() : "bg-red-500/20 text-red-300 border-red-500/30")
                .routeInfo(request.getRouteInfo())
                .imageUrl(request.getImageUrl() != null ? request.getImageUrl() : "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=1200&q=80")
                .ctaText(request.getCtaText() != null ? request.getCtaText() : "Book With Offer")
                .ctaLink(request.getCtaLink() != null ? request.getCtaLink() : "/search")
                .active(request.getActive() != null ? request.getActive() : true)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 1)
                .isAiGenerated(request.getIsAiGenerated() != null ? request.getIsAiGenerated() : false)
                .promptUsed(request.getPromptUsed())
                .createdAt(LocalDateTime.now())
                .build();

        Banner saved = bannerRepository.save(banner);
        log.info("Created banner ID {} with title '{}'", saved.getId(), saved.getTitle());
        return mapToDto(saved);
    }

    @Transactional
    public BannerDto updateBanner(Long id, CreateBannerRequest request) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner not found with ID: " + id));

        banner.setTitle(request.getTitle());
        banner.setSubtitle(request.getSubtitle());
        banner.setTag(request.getTag());
        banner.setPromoCode(request.getPromoCode());
        banner.setDiscountPercentage(request.getDiscountPercentage());
        banner.setBgGradient(request.getBgGradient());
        banner.setBadgeColor(request.getBadgeColor());
        banner.setRouteInfo(request.getRouteInfo());
        banner.setImageUrl(request.getImageUrl());
        banner.setCtaText(request.getCtaText());
        banner.setCtaLink(request.getCtaLink());
        if (request.getActive() != null) banner.setActive(request.getActive());
        if (request.getSortOrder() != null) banner.setSortOrder(request.getSortOrder());

        Banner updated = bannerRepository.save(banner);
        return mapToDto(updated);
    }

    @Transactional
    public void deleteBanner(Long id) {
        bannerRepository.deleteById(id);
        log.info("Deleted banner ID {}", id);
    }

    /**
     * AI Promotional Banner Generator:
     * Analyzes suggestive prompt, extracts entities (discount %, route, festival/theme, bus category),
     * and produces high-conversion marketing banner configuration.
     */
    public BannerDto generateAiBanner(GenerateAiBannerRequest req) {
        String prompt = req.getPrompt() != null ? req.getPrompt().trim() : "";
        String lower = prompt.toLowerCase(Locale.ROOT);

        // 1. Extract discount percentage
        int discount = 20;
        if (req.getTargetDiscount() != null && req.getTargetDiscount() > 0) {
            discount = req.getTargetDiscount();
        } else {
            Matcher m = Pattern.compile("(\\d{1,2})%").matcher(prompt);
            if (m.find()) {
                try {
                    discount = Integer.parseInt(m.group(1));
                } catch (Exception ignored) {}
            }
        }

        // 2. Extract Route if mentioned
        String route = req.getTargetRoute();
        if (route == null || route.isBlank()) {
            if (lower.contains("bangalore") && lower.contains("goa")) route = "Bangalore ⇄ Goa";
            else if (lower.contains("chennai") && lower.contains("coimbatore")) route = "Chennai ⇄ Coimbatore";
            else if (lower.contains("mumbai") && lower.contains("pune")) route = "Mumbai ⇄ Pune";
            else if (lower.contains("delhi") && lower.contains("manali")) route = "Delhi ⇄ Manali";
            else if (lower.contains("hyderabad") && lower.contains("vijayawada")) route = "Hyderabad ⇄ Vijayawada";
            else if (lower.contains("kochi") && lower.contains("bangalore")) route = "Kochi ⇄ Bangalore";
            else if (lower.contains("ahmedabad") && lower.contains("mumbai")) route = "Ahmedabad ⇄ Mumbai";
            else route = "Pan-India Super Express";
        }

        // 3. Theme & Festival detection
        String tag = "AI EXCLUSIVE DEAL";
        String title = "Special Smart Travel Savings";
        String subtitle = "Get flat discount on premium AC Sleeper & Multi-Axle buses with real-time tracking.";
        String promoCode = "REDAI" + discount;
        String bgGradient = "from-slate-950 via-red-950/80 to-slate-900";
        String badgeColor = "bg-red-500/20 text-red-300 border-red-500/30";
        String imageUrl = "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=1200&q=80";

        if (lower.contains("diwali") || lower.contains("deepavali") || lower.contains("festival") || lower.contains("festive")) {
            tag = "FESTIVE MEGA BLOWOUT";
            title = "Diwali Utsav: Flat " + discount + "% Off All Express Routes";
            subtitle = "Celebrate homecoming with loved ones. Enjoy sanitized luxury berths & guaranteed on-time guarantee.";
            promoCode = "DIWALI" + discount;
            bgGradient = "from-amber-950/90 via-orange-900/80 to-slate-950";
            badgeColor = "bg-amber-500/20 text-amber-300 border-amber-500/30";
            imageUrl = "https://images.unsplash.com/photo-1570125909232-eb263c188f7e?w=1200&q=80";
        } else if (lower.contains("summer") || lower.contains("beach") || lower.contains("vacation") || lower.contains("holiday")) {
            tag = "SUMMER GETAWAY DEALS";
            title = "Chill Out: Flat " + discount + "% Off Weekend Escapes";
            subtitle = "Direct AC Sleeper luxury coaches to scenic hills and sunny beaches at unbeatable prices.";
            promoCode = "SUMMER" + discount;
            bgGradient = "from-teal-950/90 via-cyan-950/80 to-slate-950";
            badgeColor = "bg-teal-500/20 text-teal-300 border-teal-500/30";
            imageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80";
        } else if (lower.contains("women") || lower.contains("safety") || lower.contains("solo")) {
            tag = "WOMEN SAFETY FIRST";
            title = "SafeTravel+: AI Smart Solo Seating & " + discount + "% Off";
            subtitle = "Dedicated verified safe zones, 24/7 CCTV surveillance & verified women-friendly boarding points.";
            promoCode = "HERRIDE" + discount;
            bgGradient = "from-purple-950/90 via-fuchsia-950/80 to-slate-950";
            badgeColor = "bg-purple-500/20 text-purple-300 border-purple-500/30";
            imageUrl = "https://images.unsplash.com/photo-1517649763962-0c623266ddc0?w=1200&q=80";
        } else if (lower.contains("luxury") || lower.contains("volvo") || lower.contains("sleeper") || lower.contains("scania")) {
            tag = "FIRST-CLASS EXPERIENCE";
            title = "Ultra-Luxury Volvo Sleeper: Save " + discount + "% Today";
            subtitle = "Reclining memory-foam beds, personal entertainment screens, charging ports & onboard snacks.";
            promoCode = "LUXE" + discount;
            bgGradient = "from-slate-950 via-indigo-950/80 to-slate-900";
            badgeColor = "bg-indigo-500/20 text-indigo-300 border-indigo-500/30";
            imageUrl = "https://images.unsplash.com/photo-1570125909232-eb263c188f7e?w=1200&q=80";
        } else if (lower.contains("flash") || lower.contains("midnight") || lower.contains("last minute")) {
            tag = "⚡ FLASH SALE";
            title = "Midnight Rush: Instant Flat " + discount + "% Off Next 2 Hours";
            subtitle = "Limited seats on high-demand premium buses. Book before seats sell out!";
            promoCode = "FLASH" + discount;
            bgGradient = "from-rose-950/90 via-red-900/80 to-slate-950";
            badgeColor = "bg-rose-500/20 text-rose-300 border-rose-500/30";
            imageUrl = "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=1200&q=80";
        } else if (!prompt.isBlank()) {
            title = prompt.length() > 60 ? prompt.substring(0, 57) + "..." : prompt;
            subtitle = "Exclusive promotional deal generated for: " + prompt;
            promoCode = "OFFER" + discount;
        }

        return BannerDto.builder()
                .title(title)
                .subtitle(subtitle)
                .tag(tag)
                .promoCode(promoCode)
                .discountPercentage(discount)
                .bgGradient(bgGradient)
                .badgeColor(badgeColor)
                .routeInfo(route)
                .imageUrl(imageUrl)
                .ctaText("Claim " + discount + "% Off")
                .ctaLink("/search")
                .active(true)
                .sortOrder(1)
                .isAiGenerated(true)
                .promptUsed(prompt)
                .build();
    }

    private BannerDto mapToDto(Banner banner) {
        return BannerDto.builder()
                .id(banner.getId())
                .title(banner.getTitle())
                .subtitle(banner.getSubtitle())
                .tag(banner.getTag())
                .promoCode(banner.getPromoCode())
                .discountPercentage(banner.getDiscountPercentage())
                .bgGradient(banner.getBgGradient())
                .badgeColor(banner.getBadgeColor())
                .routeInfo(banner.getRouteInfo())
                .imageUrl(banner.getImageUrl())
                .ctaText(banner.getCtaText())
                .ctaLink(banner.getCtaLink())
                .active(banner.getActive())
                .sortOrder(banner.getSortOrder())
                .isAiGenerated(banner.getIsAiGenerated())
                .promptUsed(banner.getPromptUsed())
                .createdAt(banner.getCreatedAt())
                .build();
    }
}
