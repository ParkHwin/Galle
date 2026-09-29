package com.gallae.controller;

import com.gallae.dto.ApiResponse;
import com.gallae.service.CarService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/car")
@RequiredArgsConstructor
public class CarController {

    private final CarService carService;

    @GetMapping("/directions")
    public ApiResponse<Map<String, Object>> getDirections(
            @RequestParam("origin") String origin,
            @RequestParam("destination") String destination) {
        return ApiResponse.ok(carService.getDirections(origin, destination));
    }
}
