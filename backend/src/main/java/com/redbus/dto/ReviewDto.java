package com.redbus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDto {
    private Long id;
    private Long busId;
    private Long userId;
    private String userName;
    private Integer rating;
    private String comment;
    private List<String> tags;
    private LocalDateTime createdAt;
}
