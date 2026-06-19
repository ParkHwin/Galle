package com.gallae.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FareDto {
    private String transportType;
    private String departureName;
    private String arrivalName;
    private String seatClass;
    private Long fare;
}
