package com.gallae.service;

import com.gallae.config.ExternalApiProperties;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 엑셀 시각표(resources/data/*.xlsx) 파싱 확인. API·스프링 없이 실행된다. */
class TrainScheduleCacheTest {

    private static final LocalDate THURSDAY = LocalDate.of(2026, 10, 15);

    private TrainScheduleCache loaded() {
        TrainScheduleCache cache = new TrainScheduleCache(new ExternalApiProperties());
        cache.loadSrtTimetable();
        cache.loadExcelTimetables();
        return cache;
    }

    @Test
    void 엑셀_시각표로_서울_부산_열차를_찾는다() {
        TrainScheduleCache cache = loaded();
        assertTrue(cache.hasTimetable());

        List<TrainScheduleCache.Trip> ktx = cache.findTrips("KTX", "서울", "부산", THURSDAY);
        assertFalse(ktx.isEmpty());
        TrainScheduleCache.Trip first = ktx.get(0);
        assertEquals("1", first.trainNo());             // KTX 1: 서울 05:13 → 부산 07:50
        assertEquals("05:13", first.depTime());
        assertEquals("07:50", first.arrTime());
        assertEquals(157, first.minutes());

        assertFalse(cache.findTrips("무궁화호", "서울", "부산", THURSDAY).isEmpty());
        assertFalse(cache.findTrips("SRT", "수서", "부산", THURSDAY).isEmpty());
    }

    @Test
    void 서울_검색은_용산_출발_호남선도_포함한다() {
        TrainScheduleCache cache = loaded();
        assertTrue(cache.findTrips("ITX-새마을", "서울", "광주", THURSDAY).stream()
                .anyMatch(t -> t.depStation().equals("용산")));
        assertTrue(cache.findTrips("KTX", "서울", "광주", THURSDAY).stream()
                .anyMatch(t -> t.arrStation().equals("광주송정")));
    }

    @Test
    void 운행요일을_거른다() {
        assertTrue(TrainScheduleCache.runsOn("매일", THURSDAY));
        assertTrue(TrainScheduleCache.runsOn("월화수목", THURSDAY));
        assertFalse(TrainScheduleCache.runsOn("금토일", THURSDAY));
        assertFalse(TrainScheduleCache.runsOn("토일", THURSDAY));
        assertTrue(TrainScheduleCache.runsOn("중앙선 경유", THURSDAY)); // 요일 표기 없음 → 매일
        assertTrue(TrainScheduleCache.runsOn(null, THURSDAY));
    }
}
