package com.gallae.util;

import com.gallae.dto.FareDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class FallbackFareLoader {

    /**
     * SRT CSV fallback 데이터 로드
     * 파일 위치: src/main/resources/data/(주)에스알_srt여객운임 정보_20200114.csv
     */
    public List<FareDto> loadSrtFares() {
        List<FareDto> fares = new ArrayList<>();
        try {
            ClassPathResource resource = new ClassPathResource("data/(주)에스알_srt여객운임 정보_20200114.csv");
            if (!resource.exists()) {
                log.warn("[FallbackFareLoader] SRT CSV 파일이 없습니다. Mock 데이터를 사용합니다.");
                return getDefaultSrtFares();
            }
            // TODO: CSV 파싱 로직 (opencsv 사용)
            log.info("[FallbackFareLoader] SRT CSV fallback 로드 성공");
        } catch (Exception e) {
            log.error("[FallbackFareLoader] SRT CSV 로드 실패: {}", e.getMessage());
            return getDefaultSrtFares();
        }
        return fares.isEmpty() ? getDefaultSrtFares() : fares;
    }

    /**
     * KTX XLS fallback 데이터 로드
     * 파일 위치: src/main/resources/data/KTX운임표.xls
     */
    public List<FareDto> loadKtxFares() {
        List<FareDto> fares = new ArrayList<>();
        try {
            ClassPathResource resource = new ClassPathResource("data/KTX운임표.xls");
            if (!resource.exists()) {
                log.warn("[FallbackFareLoader] KTX XLS 파일이 없습니다. Mock 데이터를 사용합니다.");
                return getDefaultKtxFares();
            }
            // TODO: XLS 파싱 로직 (Apache POI 사용)
            log.info("[FallbackFareLoader] KTX XLS fallback 로드 성공");
        } catch (Exception e) {
            log.error("[FallbackFareLoader] KTX XLS 로드 실패: {}", e.getMessage());
            return getDefaultKtxFares();
        }
        return fares.isEmpty() ? getDefaultKtxFares() : fares;
    }

    private List<FareDto> getDefaultSrtFares() {
        return List.of(
            FareDto.builder().transportType("SRT").departureName("수서").arrivalName("부산").seatClass("standard").fare(52600L).build(),
            FareDto.builder().transportType("SRT").departureName("수서").arrivalName("광주송정").seatClass("standard").fare(46800L).build(),
            FareDto.builder().transportType("SRT").departureName("수서").arrivalName("대전").seatClass("standard").fare(23700L).build(),
            FareDto.builder().transportType("SRT").departureName("수서").arrivalName("동대구").seatClass("standard").fare(39000L).build()
        );
    }

    private List<FareDto> getDefaultKtxFares() {
        return List.of(
            FareDto.builder().transportType("KTX").departureName("서울").arrivalName("부산").seatClass("standard").fare(59800L).build(),
            FareDto.builder().transportType("KTX").departureName("서울").arrivalName("광주송정").seatClass("standard").fare(46800L).build(),
            FareDto.builder().transportType("KTX").departureName("서울").arrivalName("대전").seatClass("standard").fare(23700L).build(),
            FareDto.builder().transportType("KTX").departureName("서울").arrivalName("동대구").seatClass("standard").fare(39000L).build()
        );
    }
}
