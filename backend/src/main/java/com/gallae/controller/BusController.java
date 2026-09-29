package com.gallae.controller;

import com.gallae.dto.ApiResponse;
import com.gallae.service.BusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/bus")
@RequiredArgsConstructor
public class BusController {

    private final BusService busService;

    @GetMapping("/cities")
    public ApiResponse<List<Map<String, Object>>> getCities() {
        return ApiResponse.ok(busService.getCities());
    }

    @GetMapping("/terminals")
    public ApiResponse<List<Map<String, Object>>> getTerminals(
            @RequestParam(name = "name", required = false) String name) {
        return ApiResponse.ok(busService.getTerminals(name));
    }

    @GetMapping("/grades")
    public ApiResponse<List<Map<String, Object>>> getGrades() {
        return ApiResponse.ok(busService.getGrades());
    }

    @GetMapping("/routes")
    public ApiResponse<List<Map<String, Object>>> getRoutes(
            @RequestParam("depCityCode") String depCityCode,
            @RequestParam("arrCityCode") String arrCityCode,
            @RequestParam(name = "date", required = false) String date) {
        return ApiResponse.ok(busService.getRoutes(depCityCode, arrCityCode, date));
    }
}
