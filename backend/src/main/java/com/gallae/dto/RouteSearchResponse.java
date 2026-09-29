package com.gallae.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class RouteSearchResponse {
    private String from;
    private String to;
    private String date;
    private String time;
    private List<RouteResult> results;
    private Summary summary;
    private String disclaimer;

    @Data
    @Builder
    public static class Summary {
        private String cheapest;
        private String fastest;
        private String recommended;
    }
}
