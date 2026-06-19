package com.gallae.service;

import com.gallae.dto.FareDto;
import com.gallae.util.FallbackFareLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FareService {

    private final FallbackFareLoader fallbackFareLoader;

    public List<FareDto> getSrtFares() {
        return fallbackFareLoader.loadSrtFares();
    }

    public List<FareDto> getKtxFares() {
        return fallbackFareLoader.loadKtxFares();
    }

    public List<FareDto> getNormalTrainFares() {
        // ITX-마음, ITX-새마을, 새마을호 운임표
        return List.of(
            FareDto.builder().transportType("ITX-새마을").departureName("서울").arrivalName("부산").seatClass("standard").fare(42600L).build(),
            FareDto.builder().transportType("ITX-새마을").departureName("서울").arrivalName("대전").seatClass("standard").fare(17600L).build(),
            FareDto.builder().transportType("새마을호").departureName("서울").arrivalName("부산").seatClass("standard").fare(36100L).build(),
            FareDto.builder().transportType("ITX-마음").departureName("서울").arrivalName("부산").seatClass("standard").fare(28700L).build()
        );
    }
}
