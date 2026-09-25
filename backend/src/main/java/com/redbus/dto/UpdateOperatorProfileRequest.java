package com.redbus.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOperatorProfileRequest {
    @NotBlank(message = "Company / Agency name is required")
    private String companyName;

    @NotBlank(message = "Contact person name is required")
    private String contactPerson;

    private String phone;
    private String email;
    private String bankAccountRef;
    private String kycDocUrl;
}
