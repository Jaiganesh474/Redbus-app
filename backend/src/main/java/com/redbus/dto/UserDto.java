package com.redbus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private Boolean emailVerified;
    private String avatarUrl;
    private String gender;
    private String operatorStatus;
    private java.math.BigDecimal walletBalance;
}
