package com.gallae.repository;

import com.gallae.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StationRepository extends JpaRepository<Station, Long> {
    List<Station> findByCityAndType(String city, String type);
    Optional<Station> findByNameAndType(String name, String type);
    List<Station> findByCity(String city);
}
