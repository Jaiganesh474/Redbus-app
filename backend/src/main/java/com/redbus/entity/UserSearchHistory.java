package com.redbus.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_search_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "source_city", nullable = false, length = 100)
    private String sourceCity;

    @Column(name = "destination_city", nullable = false, length = 100)
    private String destinationCity;

    @Column(name = "search_date", nullable = false)
    private LocalDate searchDate;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
