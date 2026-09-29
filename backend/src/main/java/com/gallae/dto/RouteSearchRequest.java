package com.gallae.dto;

import lombok.Data;

@Data
public class RouteSearchRequest {
    private String from;
    private String to;
    private String date;
    private String time;
}
