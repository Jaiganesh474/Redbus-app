package com.redbus.dto;

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
    private String email;
    private String phone;
    private String name;
    private String photoUrl;
}

