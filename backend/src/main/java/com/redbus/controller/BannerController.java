package com.redbus.controller;

import com.redbus.dto.BannerDto;
import com.redbus.dto.CreateBannerRequest;
import com.redbus.dto.GenerateAiBannerRequest;
import com.redbus.service.BannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/banners", "/api/v1/banners"})
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    @GetMapping
    public ResponseEntity<List<BannerDto>> getActiveBanners() {
        return ResponseEntity.ok(bannerService.getActiveBanners());
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<BannerDto>> getAllBannersAdmin() {
        return ResponseEntity.ok(bannerService.getAllBannersForAdmin());
    }

    @PostMapping("/admin")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<BannerDto> createBanner(@Valid @RequestBody CreateBannerRequest request) {
        BannerDto created = bannerService.createBanner(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<BannerDto> updateBanner(@PathVariable Long id, @Valid @RequestBody CreateBannerRequest request) {
        return ResponseEntity.ok(bannerService.updateBanner(id, request));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteBanner(@PathVariable Long id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.ok(Map.of("message", "Banner deleted successfully", "id", id));
    }

    @PostMapping("/admin/ai-generate")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<BannerDto> generateAiBanner(@Valid @RequestBody GenerateAiBannerRequest request) {
        BannerDto generated = bannerService.generateAiBanner(request);
        return ResponseEntity.ok(generated);
    }
}
