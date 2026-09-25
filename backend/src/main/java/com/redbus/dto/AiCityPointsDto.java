package com.redbus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCityPointsDto {
    private String city;
    private String boardingPoints;
    private String droppingPoints;
    private List<String> majorLandmarks;
}
