package com.gallae.controller;

import com.gallae.dto.ApiResponse;
import com.gallae.dto.FareDto;
import com.gallae.service.FareService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/fares")
@RequiredArgsConstructor
public class FareController {

    private final FareService fareService;

    @GetMapping("/srt")
    public ApiResponse<List<FareDto>> getSrtFares() {
        return ApiResponse.ok(fareService.getSrtFares());
    }

    @GetMapping("/ktx")
    public ApiResponse<List<FareDto>> getKtxFares() {
        return ApiResponse.ok(fareService.getKtxFares());
    }

    @GetMapping("/normal-train")
    public ApiResponse<List<FareDto>> getNormalTrainFares() {
        return ApiResponse.ok(fareService.getNormalTrainFares());
    }
}
