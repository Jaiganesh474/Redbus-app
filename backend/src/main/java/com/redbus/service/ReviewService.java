package com.redbus.service;

import com.redbus.dto.CreateReviewRequest;
import com.redbus.dto.ReviewDto;
import com.redbus.dto.ReviewSummaryDto;
import com.redbus.entity.Bus;
import com.redbus.entity.Review;
import com.redbus.entity.User;
import com.redbus.exception.ResourceNotFoundException;
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

    @Transactional
    public ReviewSummaryDto getReviewSummary(Long busId) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + busId));

        List<Review> reviews = reviewRepository.findByBusIdOrderByCreatedAtDesc(busId);

        // Seed realistic default verified traveler reviews if none exist
        if (reviews.isEmpty()) {
            reviews = seedInitialReviews(bus);
        }

        // Calculate aggregate statistics
        double avg = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(bus.getRating() != null ? bus.getRating().doubleValue() : 4.4);

        int totalCount = Math.max(reviews.size(), 427); // Baseline realistic Redbus ratings count

        // Star breakdown percentages
        Map<Integer, Integer> starPercentages = new LinkedHashMap<>();
        starPercentages.put(5, 71);
        starPercentages.put(4, 14);
        starPercentages.put(3, 3);
        starPercentages.put(2, 3);
        starPercentages.put(1, 8);

        // Dynamic adjustment if actual reviews exist
        if (reviews.size() > 5) {
            long c5 = reviews.stream().filter(r -> r.getRating() == 5).count();
            long c4 = reviews.stream().filter(r -> r.getRating() == 4).count();
            long c3 = reviews.stream().filter(r -> r.getRating() == 3).count();
            long c2 = reviews.stream().filter(r -> r.getRating() == 2).count();
            long c1 = reviews.stream().filter(r -> r.getRating() == 1).count();
            int total = reviews.size();

            starPercentages.put(5, (int) Math.round((double) c5 / total * 100));
            starPercentages.put(4, (int) Math.round((double) c4 / total * 100));
            starPercentages.put(3, (int) Math.round((double) c3 / total * 100));
            starPercentages.put(2, (int) Math.round((double) c2 / total * 100));
            starPercentages.put(1, (int) Math.round((double) c1 / total * 100));
        }

        // Loved tags mapping (matching Redbus UI)
        Map<String, Integer> lovedTags = new LinkedHashMap<>();
        lovedTags.put("Punctuality", 210);
        lovedTags.put("Staff behavior", 181);
        lovedTags.put("Seat / Sleep Comfort", 179);
        lovedTags.put("Driving", 178);
        lovedTags.put("Cleanliness", 174);
        lovedTags.put("Rest stop hygiene", 171);
        lovedTags.put("AC", 155);
        lovedTags.put("Live tracking", 151);

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

        Review review = Review.builder()
                .bus(bus)
                .user(user)
                .userName(reviewerName)
                .rating(request.getRating())
                .comment(request.getComment() != null ? request.getComment().trim() : "")
                .tags(tagsStr)
                .createdAt(LocalDateTime.now())
                .build();

        Review saved = reviewRepository.save(review);

        // Recalculate bus average rating
        List<Review> allReviews = reviewRepository.findByBusIdOrderByCreatedAtDesc(bus.getId());
        double newAvg = allReviews.stream().mapToInt(Review::getRating).average().orElse(request.getRating());
        bus.setRating(BigDecimal.valueOf(Math.round(newAvg * 10.0) / 10.0));
        busRepository.save(bus);

        return mapToDto(saved);
    }

    private List<Review> seedInitialReviews(Bus bus) {
        List<Review> seeded = new ArrayList<>();

        Review r1 = Review.builder()
                .bus(bus)
                .userName("Arun Kumar R.")
                .rating(5)
                .comment("Excellent Volvo journey! Bus was on time at Madiwala. Clean blankets, working charging port, and very smooth driving overnight.")
                .tags("Punctuality,Driving,Cleanliness,AC")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        Review r2 = Review.builder()
                .bus(bus)
                .userName("Priya Venkatesh")
                .rating(5)
                .comment("Safe for solo female travelers. The sleeper berth was spacious and curtains provided full privacy. Driver was courteous.")
                .tags("Seat / Sleep Comfort,Staff behavior,Live tracking")
                .createdAt(LocalDateTime.now().minusDays(3))
                .build();

        Review r3 = Review.builder()
                .bus(bus)
                .userName("Karthik Subramanian")
                .rating(4)
                .comment("Good rest stop at Shri Balaji Bhavan. Food and washrooms were clean. Arrived in Chennai 15 minutes ahead of schedule.")
                .tags("Punctuality,Rest stop hygiene,Food Quality")
                .createdAt(LocalDateTime.now().minusDays(5))
                .build();

        Review r4 = Review.builder()
                .bus(bus)
                .userName("Divya N.")
                .rating(4)
                .comment("Pleasant travel experience. Water bottle provided. Smooth online ticket confirmation via redBus.")
                .tags("Staff behavior,Seat / Sleep Comfort")
                .createdAt(LocalDateTime.now().minusDays(7))
                .build();

        seeded.add(reviewRepository.save(r1));
        seeded.add(reviewRepository.save(r2));
        seeded.add(reviewRepository.save(r3));
        seeded.add(reviewRepository.save(r4));

        return seeded;
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
