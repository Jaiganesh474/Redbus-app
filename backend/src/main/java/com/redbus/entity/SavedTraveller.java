package com.redbus.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "saved_travellers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedTraveller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer age;

    @Column(nullable = false, length = 20)
    private String gender; // 'MALE', 'FEMALE', 'OTHER'

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
