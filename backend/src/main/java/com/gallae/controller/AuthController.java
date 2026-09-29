package com.gallae.controller;

import com.gallae.dto.ApiResponse;
import com.gallae.dto.UserDto;
import com.gallae.entity.User;
import com.gallae.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;

    /** 현재 로그인 사용자 정보 조회. JWT 없으면 401. */
    @GetMapping("/me")
    public ApiResponse<UserDto> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Long userId = (Long) authentication.getPrincipal();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ApiResponse.ok(UserDto.from(user));
    }
}
