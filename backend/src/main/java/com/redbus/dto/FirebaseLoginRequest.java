package com.redbus.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirebaseLoginRequest {
    private String idToken;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String name;
    private String photoUrl;
}
