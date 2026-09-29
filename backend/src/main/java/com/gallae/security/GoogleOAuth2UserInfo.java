package com.gallae.security;

import java.util.Map;

public class GoogleOAuth2UserInfo extends OAuth2UserInfo {

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getProviderId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getNickname() {
        String name = (String) attributes.get("name");
        return name != null ? name : "Google 사용자";
    }

    @Override
    public String getProfileImageUrl() {
        return (String) attributes.get("picture");
    }
}
