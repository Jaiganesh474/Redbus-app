package com.redbus.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorRegisterRequest {

    @NotBlank(message = "Company or Agency Name is required")
    private String companyName;

    @com.fasterxml.jackson.annotation.JsonAlias({"contactName", "contactPerson", "name"})
    @NotBlank(message = "Contact Person Name is required")
    private String contactPerson;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
}
