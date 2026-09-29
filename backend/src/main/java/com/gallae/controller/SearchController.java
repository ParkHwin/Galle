package com.gallae.controller;

import com.gallae.dto.ApiResponse;
import com.gallae.dto.RouteSearchResponse;
import com.gallae.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/routes")
    public ApiResponse<RouteSearchResponse> searchRoutes(
            @RequestParam("from") String from,
            @RequestParam("to") String to,
            @RequestParam(name = "date", required = false) String date,
            @RequestParam(name = "time", required = false) String time) {
        RouteSearchResponse response = searchService.search(from, to, date, time);
        return ApiResponse.ok(response);
    }
}
