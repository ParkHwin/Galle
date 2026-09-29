package com.gallae.service;

import com.gallae.dto.FavoriteDto;
import com.gallae.dto.FavoriteRequest;
import com.gallae.entity.Favorite;
import com.gallae.entity.User;
import com.gallae.repository.FavoriteRepository;
import com.gallae.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<FavoriteDto> list(Long userId) {
        return favoriteRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(FavoriteDto::from)
                .toList();
    }

    @Transactional
    public FavoriteDto add(Long userId, FavoriteRequest request) {
        if (request.getFrom() == null || request.getFrom().isBlank()
                || request.getTo() == null || request.getTo().isBlank()) {
            throw new IllegalArgumentException("출발지와 도착지는 필수입니다.");
        }
        if (request.getFrom().equals(request.getTo())) {
            throw new IllegalArgumentException("출발지와 도착지가 같습니다.");
        }

        // 이미 즐겨찾기된 노선이면 기존 항목을 그대로 반환 (멱등)
        Favorite existing = favoriteRepository
                .findByUser_IdAndDepartureCityAndArrivalCity(userId, request.getFrom(), request.getTo())
                .orElse(null);
        if (existing != null) {
            return FavoriteDto.from(existing);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        String label = (request.getLabel() != null && !request.getLabel().isBlank())
                ? request.getLabel()
                : request.getFrom() + " → " + request.getTo();

        Favorite favorite = Favorite.builder()
                .user(user)
                .departureCity(request.getFrom())
                .arrivalCity(request.getTo())
                .label(label)
                .build();

        return FavoriteDto.from(favoriteRepository.save(favorite));
    }

    @Transactional
    public void remove(Long userId, String from, String to) {
        favoriteRepository.deleteByUser_IdAndDepartureCityAndArrivalCity(userId, from, to);
    }
}
