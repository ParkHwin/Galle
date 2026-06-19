package com.gallae.repository;

import com.gallae.entity.Fare;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FareRepository extends JpaRepository<Fare, Long> {
    Optional<Fare> findByTransportTypeAndDeparture_IdAndArrival_IdAndClassType(
        String transportType, Long departureId, Long arrivalId, String classType);
    List<Fare> findByTransportTypeAndDeparture_CityAndArrival_City(
        String transportType, String departureCity, String arrivalCity);
}
