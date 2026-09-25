package com.redbus.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "route_seats", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"route_id", "seat_id"})
}, indexes = {
    @Index(name = "idx_route_seats_status", columnList = "route_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "AVAILABLE"; // 'AVAILABLE', 'LOCKED', 'BOOKED'

    @Column(name = "gender_restriction", length = 10)
    @Builder.Default
    private String genderRestriction = "NONE"; // 'NONE', 'FEMALE', 'MALE'

    @Column(name = "booked_gender", length = 20)
    private String bookedGender; // 'FEMALE', 'MALE'

    @Column(name = "lock_expiry")
    private LocalDateTime lockExpiry;

    @Column(name = "locked_by_user_id")
    private Long lockedByUserId;

    @Column(name = "price_override", precision = 10, scale = 2)
    private BigDecimal priceOverride;
}
