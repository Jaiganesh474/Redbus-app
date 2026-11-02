package com.redbus.service;

import com.redbus.dto.CreateReviewRequest;
import com.redbus.dto.ReviewDto;
import com.redbus.dto.ReviewSummaryDto;
import com.redbus.entity.Bus;
import com.redbus.entity.Review;
import com.redbus.entity.User;
import com.redbus.exception.ResourceNotFoundException;
import com.redbus.repository.BookingRepository;
import com.redbus.repository.BusRepository;
import com.redbus.repository.ReviewRepository;
import com.redbus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BusRepository busRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public ReviewSummaryDto getReviewSummary(Long busId) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + busId));

        List<Review> reviews = reviewRepository.findByBusIdOrderByCreatedAtDesc(busId).stream()
                .filter(r -> !isMockReview(r))
                .collect(Collectors.toList());

        double avg;
        int totalCount = reviews.size();
        if (!reviews.isEmpty()) {
            avg = reviews.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);
            avg = Math.round(avg * 10.0) / 10.0;
            bus.setRating(BigDecimal.valueOf(avg));
            busRepository.save(bus);
        } else {
            avg = bus.getRating() != null ? bus.getRating().doubleValue() : 0.0;
        }

        // Star breakdown percentages
        Map<Integer, Integer> starPercentages = new LinkedHashMap<>();
        if (totalCount > 0) {
            long c5 = reviews.stream().filter(r -> r.getRating() == 5).count();
            long c4 = reviews.stream().filter(r -> r.getRating() == 4).count();
            long c3 = reviews.stream().filter(r -> r.getRating() == 3).count();
            long c2 = reviews.stream().filter(r -> r.getRating() == 2).count();
            long c1 = reviews.stream().filter(r -> r.getRating() == 1).count();
            starPercentages.put(5, (int) Math.round((double) c5 / totalCount * 100));
            starPercentages.put(4, (int) Math.round((double) c4 / totalCount * 100));
            starPercentages.put(3, (int) Math.round((double) c3 / totalCount * 100));
            starPercentages.put(2, (int) Math.round((double) c2 / totalCount * 100));
            starPercentages.put(1, (int) Math.round((double) c1 / totalCount * 100));
        } else {
            starPercentages.put(5, 0);
            starPercentages.put(4, 0);
            starPercentages.put(3, 0);
            starPercentages.put(2, 0);
            starPercentages.put(1, 0);
        }

        Map<String, Integer> lovedTags = new LinkedHashMap<>();
        if (totalCount > 0) {
            lovedTags.put("Punctuality", totalCount);
            lovedTags.put("Staff behavior", totalCount);
            lovedTags.put("Seat / Sleep Comfort", totalCount);
            lovedTags.put("Cleanliness", totalCount);
        }

        List<ReviewDto> dtos = reviews.stream().map(this::mapToDto).collect(Collectors.toList());

        return ReviewSummaryDto.builder()
                .averageRating(Math.round(avg * 10.0) / 10.0)
                .totalRatings(totalCount)
                .starPercentages(starPercentages)
                .lovedTags(lovedTags)
                .reviews(dtos)
                .build();
    }

    @Transactional
    public ReviewDto addReview(CreateReviewRequest request, Long userId, String authenticatedUserName) {
        Bus bus = busRepository.findById(request.getBusId())
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + request.getBusId()));

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        String reviewerName = request.getUserName();
        if (reviewerName == null || reviewerName.isBlank()) {
            reviewerName = (user != null && user.getName() != null) ? user.getName() : authenticatedUserName;
        }
        if (reviewerName == null || reviewerName.isBlank()) {
            reviewerName = "Verified Passenger";
        }

        String tagsStr = (request.getTags() != null && !request.getTags().isEmpty())
                ? String.join(",", request.getTags())
                : "Punctuality,Seat / Sleep Comfort";

        // Prevent duplicate review count from the same passenger/user
        Review existing = null;
        if (user != null) {
            existing = reviewRepository.findFirstByBusIdAndUserId(bus.getId(), user.getId()).orElse(null);
        }
        if (existing == null && reviewerName != null && !reviewerName.isBlank()) {
            existing = reviewRepository.findFirstByBusIdAndUserName(bus.getId(), reviewerName).orElse(null);
        }

        Review saved;
        if (existing != null) {
            existing.setRating(request.getRating());
            existing.setComment(request.getComment() != null ? request.getComment().trim() : "");
            existing.setTags(tagsStr);
            existing.setCreatedAt(LocalDateTime.now());
            saved = reviewRepository.save(existing);
        } else {
            Review review = Review.builder()
                    .bus(bus)
                    .user(user)
                    .userName(reviewerName)
                    .rating(request.getRating())
                    .comment(request.getComment() != null ? request.getComment().trim() : "")
                    .tags(tagsStr)
                    .createdAt(LocalDateTime.now())
                    .build();
            saved = reviewRepository.save(review);
        }

        // If PNR was provided, link and persist the rating onto the Booking entity
        if (request.getPnr() != null && !request.getPnr().isBlank()) {
            bookingRepository.findByPnr(request.getPnr().trim().toUpperCase()).ifPresent(b -> {
                b.setUserRating(request.getRating());
                b.setUserReview(request.getComment() != null ? request.getComment().trim() : "");
                bookingRepository.save(b);
            });
        }

        // Recalculate bus average rating with verified non-mock reviews
        List<Review> allReviews = reviewRepository.findByBusIdOrderByCreatedAtDesc(bus.getId()).stream()
                .filter(r -> !isMockReview(r))
                .collect(Collectors.toList());
        double newAvg = allReviews.stream().mapToInt(Review::getRating).average().orElse(request.getRating());
        bus.setRating(BigDecimal.valueOf(Math.round(newAvg * 10.0) / 10.0));
        busRepository.save(bus);

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> getBusReviews(Long busId) {
        return reviewRepository.findByBusIdOrderByCreatedAtDesc(busId)
                .stream()
                .filter(r -> !isMockReview(r))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private boolean isMockReview(Review r) {
        String name = r.getUserName();
        if (name == null) return false;
        String trimmed = name.trim();
        return trimmed.equalsIgnoreCase("Arun Kumar R.")
                || trimmed.equalsIgnoreCase("Priya Venkatesh")
                || trimmed.equalsIgnoreCase("Karthik Subramanian")
                || trimmed.equalsIgnoreCase("Divya N.");
    }


    private ReviewDto mapToDto(Review r) {
        List<String> tagsList = Collections.emptyList();
        if (r.getTags() != null && !r.getTags().isBlank()) {
            tagsList = Arrays.stream(r.getTags().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        return ReviewDto.builder()
                .id(r.getId())
                .busId(r.getBus().getId())
                .userId(r.getUser() != null ? r.getUser().getId() : null)
                .userName(r.getUserName())
                .rating(r.getRating())
                .comment(r.getComment())
                .tags(tagsList)
                .createdAt(r.getCreatedAt())
                .build();
    }
}
