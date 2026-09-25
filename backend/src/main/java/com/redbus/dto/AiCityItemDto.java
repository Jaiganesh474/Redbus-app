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
public class AiCityItemDto {
    private String name;
    private String state;
    private String tag;
    private List<String> aliases;
    private int activeRoutesCount;
}
