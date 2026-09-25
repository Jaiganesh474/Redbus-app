package com.redbus.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seats", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"bus_id", "seat_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_id", nullable = false)
    private Bus bus;

    @Column(name = "seat_number", nullable = false, length = 10)
    private String seatNumber;

    @Column(name = "seat_type", nullable = false, length = 20)
    private String seatType; // 'SEATER', 'SLEEPER'

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String deck = "LOWER"; // 'LOWER', 'UPPER'

    @Column(name = "row_num", nullable = false)
    @Builder.Default
    private Integer rowNum = 1;

    @Column(name = "col_num", nullable = false)
    @Builder.Default
    private Integer colNum = 1;
}
