package com.redbus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSummaryDto {
    private Double averageRating;
    private Integer totalRatings;
    private Map<Integer, Integer> starPercentages; // 5 -> 71, 4 -> 14, etc.
    private Map<String, Integer> lovedTags; // "Punctuality" -> 210, etc.
    private List<ReviewDto> reviews;
}
