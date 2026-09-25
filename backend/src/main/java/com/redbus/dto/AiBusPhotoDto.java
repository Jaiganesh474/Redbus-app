package com.redbus.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiBusPhotoDto {
    private String photoUrl;
    private String photoType; // EXTERIOR, SLEEPER_CABIN, SEATER_ROW, COCKPIT, AMENITY
    private String title;
    private String description;
    private String promptUsed;
    private Double qualityScore;
}
