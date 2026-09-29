package com.gallae.repository;

import com.gallae.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<Favorite> findByUser_IdAndDepartureCityAndArrivalCity(
            Long userId, String departureCity, String arrivalCity);

    boolean existsByUser_IdAndDepartureCityAndArrivalCity(
            Long userId, String departureCity, String arrivalCity);

    void deleteByUser_IdAndDepartureCityAndArrivalCity(
            Long userId, String departureCity, String arrivalCity);
}
