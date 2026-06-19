package com.gallae.service;

import com.gallae.client.KorailClient;
import com.gallae.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainService {

    private final KorailClient korailClient;

    public List<Map<String, Object>> getCodes() {
        try {
            return korailClient.fetchTrainCodes();
        } catch (ExternalApiException e) {
            log.warn("[TrainService] 코레일 코드 조회 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> getRunPlans(String date, String depCode, String arrCode) {
        try {
            return korailClient.fetchTrainRunPlan(date, depCode, arrCode);
        } catch (ExternalApiException e) {
            log.warn("[TrainService] 운행계획 조회 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> getRunInfo(String date, String trainNo) {
        try {
            return korailClient.fetchTrainRunInfo(date, trainNo);
        } catch (ExternalApiException e) {
            log.warn("[TrainService] 운행정보 조회 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
