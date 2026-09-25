package com.redbus.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "operators")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Operator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(name = "contact_person", nullable = false, length = 100)
    private String contactPerson;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "kyc_doc_url", length = 255)
    private String kycDocUrl;

    @Column(name = "bank_account_ref", length = 100)
    private String bankAccountRef;

    @Column(name = "commission_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal commissionRate = new BigDecimal("10.00");

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "APPROVED"; // 'PENDING_APPROVAL', 'APPROVED', 'SUSPENDED'

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
