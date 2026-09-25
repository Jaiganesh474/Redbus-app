package com.redbus.controller;

import com.redbus.dto.CreateReviewRequest;
import com.redbus.dto.ReviewDto;
import com.redbus.dto.ReviewSummaryDto;
import com.redbus.entity.User;
import com.redbus.repository.UserRepository;
import com.redbus.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/reviews", "/api/v1/reviews"})
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    @GetMapping("/bus/{busId}")
    public ResponseEntity<ReviewSummaryDto> getBusReviews(@PathVariable Long busId) {
        ReviewSummaryDto summary = reviewService.getReviewSummary(busId);
        return ResponseEntity.ok(summary);
    }

    @PostMapping
    public ResponseEntity<ReviewDto> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            Authentication authentication
    ) {
        Long userId = null;
        String userName = null;
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user != null) {
                userId = user.getId();
                userName = user.getName();
            }
        }

        ReviewDto created = reviewService.addReview(request, userId, userName);
        return ResponseEntity.ok(created);
    }
}
