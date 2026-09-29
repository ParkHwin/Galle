package com.gallae.controller;

import com.gallae.dto.ApiResponse;
import com.gallae.dto.FavoriteDto;
import com.gallae.dto.FavoriteRequest;
import com.gallae.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * 로그인 사용자의 즐겨찾기 노선. 비로그인 사용자는 프론트엔드 localStorage로만 동작하며
 * 이 API를 호출하지 않는다 (SecurityConfig에서 permitAll 목록에 없어 JWT 인증이 필수다).
 */
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    public ApiResponse<List<FavoriteDto>> list(Authentication authentication) {
        return ApiResponse.ok(favoriteService.list(requireUserId(authentication)));
    }

    @PostMapping
    public ApiResponse<FavoriteDto> add(Authentication authentication, @RequestBody FavoriteRequest request) {
        return ApiResponse.ok(favoriteService.add(requireUserId(authentication), request));
    }

    @DeleteMapping
    public ApiResponse<Void> remove(
            Authentication authentication,
            @RequestParam("from") String from,
            @RequestParam("to") String to) {
        favoriteService.remove(requireUserId(authentication), from, to);
        return ApiResponse.ok(null);
    }

    private Long requireUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return (Long) authentication.getPrincipal();
    }
}
