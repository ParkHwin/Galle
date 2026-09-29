package com.gallae.util;

/**
 * 시각 관련 공통 유틸.
 * SearchService, MockDataProvider 등에서 중복 사용되던 메서드 통합.
 */
public final class TimeUtil {

    private TimeUtil() {}

    /**
     * "HH:mm" 또는 "HH:mm:ss" → 분(0~1439) 변환.
     * null이거나 파싱 불가 시 0 반환.
     */
    public static int parseTimeToMinutes(String time) {
        if (time == null || !time.contains(":")) return 0;
        try {
            String[] parts = time.split(":");
            return Integer.parseInt(parts[0].trim()) * 60 + Integer.parseInt(parts[1].trim());
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 분(0~1439) → "HH:mm" 변환.
     */
    public static String minutesToTime(int totalMinutes) {
        return String.format("%02d:%02d", (totalMinutes / 60) % 24, totalMinutes % 60);
    }
}
