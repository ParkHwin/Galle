package com.gallae.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RouteResult {
    private String type;
    private String name;
    private String departureName;
    private String arrivalName;
    private String departureTime;
    private String arrivalTime;
    private Integer durationMinutes;
    private Long fare;
    private List<String> recommendTags;
    private String bookingUrl;
    private CarDetail detail;

    @Data
    @Builder
    public static class CarDetail {
        private Double distanceKm;
        private Long toll;
        private Long fuelCost;
        private String fuelStandard;
    }
}
