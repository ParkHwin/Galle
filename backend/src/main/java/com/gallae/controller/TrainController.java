package com.gallae.controller;

import com.gallae.dto.ApiResponse;
import com.gallae.service.TrainService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/train")
@RequiredArgsConstructor
public class TrainController {

    private final TrainService trainService;

    @GetMapping("/codes")
    public ApiResponse<List<Map<String, Object>>> getCodes() {
        return ApiResponse.ok(trainService.getCodes());
    }

    @GetMapping("/plans")
    public ApiResponse<List<Map<String, Object>>> getRunPlans(
            @RequestParam String date,
            @RequestParam String depCode,
            @RequestParam String arrCode) {
        return ApiResponse.ok(trainService.getRunPlans(date, depCode, arrCode));
    }

    @GetMapping("/runs")
    public ApiResponse<List<Map<String, Object>>> getRunInfo(
            @RequestParam String date,
            @RequestParam String trainNo) {
        return ApiResponse.ok(trainService.getRunInfo(date, trainNo));
    }
}
