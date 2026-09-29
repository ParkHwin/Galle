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
        // ITX-새마을 / ITX-마음 / 무궁화호 운임표 (FallbackFareLoader에서 XLS/XLSX 파싱)
        return fallbackFareLoader.loadItxFares();
    }
}
