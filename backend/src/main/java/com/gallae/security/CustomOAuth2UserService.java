package com.gallae.security;

import com.gallae.entity.Role;
import com.gallae.entity.User;
import com.gallae.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = resolveUserInfo(registrationId, oAuth2User.getAttributes());

        User user = saveOrUpdate(registrationId, userInfo);

        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("userId", user.getId());

        String userNameAttr = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                attributes,
                userNameAttr
        );
    }

    private OAuth2UserInfo resolveUserInfo(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId) {
            case "kakao"  -> new KakaoOAuth2UserInfo(attributes);
            case "naver"  -> new NaverOAuth2UserInfo(attributes);
            case "google" -> new GoogleOAuth2UserInfo(attributes);
            default -> throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인: " + registrationId);
        };
    }

    private User saveOrUpdate(String provider, OAuth2UserInfo info) {
        return userRepository.findByProviderAndProviderId(provider, info.getProviderId())
                .map(user -> {
                    user.setNickname(info.getNickname() != null ? info.getNickname() : user.getNickname());
                    user.setProfileImageUrl(info.getProfileImageUrl());
                    return userRepository.save(user);
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .provider(provider)
                        .providerId(info.getProviderId())
                        .email(info.getEmail())
                        .nickname(info.getNickname() != null ? info.getNickname() : provider + " 사용자")
                        .profileImageUrl(info.getProfileImageUrl())
                        .role(Role.USER)
                        .build()));
    }
}
