package com.gallae.dto;

import com.gallae.entity.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDto {
    private Long id;
    private String provider;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private String role;

    public static UserDto from(User user) {
        return UserDto.builder()
                .id(user.getId())
                .provider(user.getProvider())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .role(user.getRole().name())
                .build();
    }
}
