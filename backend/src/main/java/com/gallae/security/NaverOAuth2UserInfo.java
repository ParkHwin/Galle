package com.gallae.security;

import java.util.Collections;
import java.util.Map;

public class NaverOAuth2UserInfo extends OAuth2UserInfo {

    @SuppressWarnings("unchecked")
    public NaverOAuth2UserInfo(Map<String, Object> attributes) {
        // 네이버는 사용자 정보를 "response" 키 안에 감싸서 반환
        super(attributes.get("response") instanceof Map
                ? (Map<String, Object>) attributes.get("response")
                : Collections.emptyMap());
    }

    @Override
    public String getProviderId() {
        return (String) attributes.get("id");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getNickname() {
        String name = (String) attributes.get("name");
        return name != null ? name : "네이버 사용자";
    }

    @Override
    public String getProfileImageUrl() {
        return (String) attributes.get("profile_image");
    }
}
