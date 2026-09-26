package com.redbus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDeviceSessionDto {
    private Long id;
    private String deviceName;
    private String deviceType;
    private String browser;
    private String operatingSystem;
    private String ipAddress;
    private String maskedIp;
    private String location;
    private Boolean isCurrent;
    private Boolean isActive;
    private LocalDateTime lastActive;
    private LocalDateTime createdAt;
}
