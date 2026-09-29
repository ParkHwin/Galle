package com.gallae.util;

import com.gallae.dto.FareDto;
import com.opencsv.CSVReader;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 운임 CSV/XLS를 애플리케이션 시작 시 한 번만 로드하여 캐싱.
 * loadXxx() 메서드는 캐시를 그대로 반환하므로 반복 파싱 없음.
 */
@Slf4j
@Component
public class FallbackFareLoader {

    private List<FareDto> cachedKtxFares      = Collections.emptyList();
    private List<FareDto> cachedMugunghwaFares = Collections.emptyList();
    private List<FareDto> cachedItxFares       = Collections.emptyList();
    private List<FareDto> cachedSrtFares       = Collections.emptyList();

    @PostConstruct
    public void init() {
        cachedKtxFares      = parseHssfXls("data/KTX운임표.xls",      "KTX",      1, 2, 3);
        cachedMugunghwaFares= parseHssfXls("data/무궁화호운임표.xls", "무궁화호",  1, 2, 3);
        cachedItxFares      = parseXssfXlsx("data/ITX운임표.xlsx",    "ITX-새마을",1, 2, 3);
        cachedSrtFares      = parseSrtCsv();

        if (cachedKtxFares.isEmpty())       cachedKtxFares      = defaultKtxFares();
        if (cachedMugunghwaFares.isEmpty()) cachedMugunghwaFares= defaultMugunghwaFares();
        if (cachedItxFares.isEmpty())       cachedItxFares      = defaultItxFares();
        if (cachedSrtFares.isEmpty())       cachedSrtFares      = defaultSrtFares();

        log.info("[FallbackFareLoader] 운임 로드 완료 — KTX:{}건 무궁화호:{}건 ITX:{}건 SRT:{}건",
                cachedKtxFares.size(), cachedMugunghwaFares.size(),
                cachedItxFares.size(), cachedSrtFares.size());
    }

    public List<FareDto> loadKtxFares()       { return cachedKtxFares; }
    public List<FareDto> loadMugunghwaFares()  { return cachedMugunghwaFares; }
    public List<FareDto> loadItxFares()        { return cachedItxFares; }
    public List<FareDto> loadSrtFares()        { return cachedSrtFares; }

    // ── 파서 ──────────────────────────────────────────────────────────────────

    /**
     * HSSF (.xls) 파서.
     * 구조: col[depCol]=출발역, col[arrCol]=도착역, col[fareCol]=운임(NUMERIC)
     */
    private List<FareDto> parseHssfXls(String path, String type, int depCol, int arrCol, int fareCol) {
        ClassPathResource res = new ClassPathResource(path);
        if (!res.exists()) {
            log.warn("[FallbackFareLoader] {} 없음", path);
            return Collections.emptyList();
        }
        List<FareDto> list = new ArrayList<>();
        try (HSSFWorkbook wb = new HSSFWorkbook(res.getInputStream())) {
            for (int si = 0; si < wb.getNumberOfSheets(); si++) {
                Sheet sheet = wb.getSheetAt(si);
                for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;
                    String dep  = cellStr(row.getCell(depCol));
                    String arr  = cellStr(row.getCell(arrCol));
                    if (dep.isEmpty() || arr.isEmpty()) continue;
                    if (dep.chars().anyMatch(Character::isDigit) || arr.chars().anyMatch(Character::isDigit)) continue;
                    if (dep.contains("※") || dep.contains("구간") || dep.contains("~") || dep.contains("출발역")) continue;
                    Cell fareCell = row.getCell(fareCol);
                    if (fareCell == null) continue;
                    long fare;
                    if (fareCell.getCellType() == CellType.NUMERIC) {
                        fare = (long) fareCell.getNumericCellValue();
                    } else if (fareCell.getCellType() == CellType.STRING) {
                        String s = fareCell.getStringCellValue().replaceAll("[^0-9]", "");
                        if (s.isEmpty()) continue;
                        fare = Long.parseLong(s);
                    } else continue;
                    if (fare <= 0) continue;
                    list.add(FareDto.builder()
                            .transportType(type).departureName(dep).arrivalName(arr)
                            .seatClass("standard").fare(fare).build());
                }
            }
            log.info("[FallbackFareLoader] {} 파싱 완료: {}건 (시트 {}개)", path, list.size(), wb.getNumberOfSheets());
        } catch (Exception e) {
            log.error("[FallbackFareLoader] {} 파싱 실패: {}", path, e.getMessage());
        }
        return list;
    }

    /**
     * XSSF (.xlsx) 파서 — ITX 운임표
     * 구조: col[1]=출발역, col[2]=도착역, col[3]=운임
     * 여러 시트(노선별) 모두 순회
     */
    private List<FareDto> parseXssfXlsx(String path, String type, int depCol, int arrCol, int fareCol) {
        ClassPathResource res = new ClassPathResource(path);
        if (!res.exists()) {
            log.warn("[FallbackFareLoader] {} 없음", path);
            return Collections.emptyList();
        }
        List<FareDto> list = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        try (XSSFWorkbook wb = new XSSFWorkbook(res.getInputStream())) {
            for (int si = 0; si < wb.getNumberOfSheets(); si++) {
                Sheet sheet = wb.getSheetAt(si);
                for (Row row : sheet) {
                    String dep  = cellStr(row.getCell(depCol));
                    String arr  = cellStr(row.getCell(arrCol));
                    if (dep.isEmpty() || arr.isEmpty()) continue;
                    if (dep.chars().anyMatch(Character::isDigit) || arr.chars().anyMatch(Character::isDigit)) continue;
                    if (dep.contains("※") || dep.contains("구간") || dep.contains("~") || dep.contains("출발역")) continue;
                    Cell fareCell = row.getCell(fareCol);
                    if (fareCell == null) continue;
                    long fare;
                    if (fareCell.getCellType() == CellType.NUMERIC) {
                        fare = (long) fareCell.getNumericCellValue();
                    } else if (fareCell.getCellType() == CellType.STRING) {
                        String s = fareCell.getStringCellValue().replaceAll("[^0-9]", "");
                        if (s.isEmpty()) continue;
                        fare = Long.parseLong(s);
                    } else continue;
                    if (fare <= 0) continue;
                    // 같은 구간 중복 시 첫 번째(더 큰 운임=ITX-새마을) 우선
                    String key = dep + "|" + arr;
                    if (seen.add(key)) {
                        list.add(FareDto.builder()
                                .transportType(type).departureName(dep).arrivalName(arr)
                                .seatClass("standard").fare(fare).build());
                    }
                }
            }
            log.info("[FallbackFareLoader] {} 파싱 완료: {}건 (시트 {}개)", path, list.size(), wb.getNumberOfSheets());
        } catch (Exception e) {
            log.error("[FallbackFareLoader] {} 파싱 실패: {}", path, e.getMessage());
        }
        return list;
    }

    /** SRT CSV 파서 */
    private List<FareDto> parseSrtCsv() {
        ClassPathResource res = new ClassPathResource("data/srt_fares.csv");
        if (!res.exists()) {
            log.warn("[FallbackFareLoader] srt_fares.csv 없음");
            return Collections.emptyList();
        }
        List<FareDto> list = new ArrayList<>();
        try (CSVReader reader = new CSVReader(
                new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
            String[] row;
            boolean header = true;
            while ((row = reader.readNext()) != null) {
                if (header) { header = false; continue; }
                if (row.length < 3) continue;
                String dep = row[0].trim(); String arr = row[1].trim();
                String fareStr = row[2].trim().replaceAll("[^0-9]", "");
                if (dep.isEmpty() || arr.isEmpty() || fareStr.isEmpty()) continue;
                list.add(FareDto.builder()
                        .transportType("SRT").departureName(dep).arrivalName(arr)
                        .seatClass("standard").fare(Long.parseLong(fareStr)).build());
            }
        } catch (Exception e) {
            log.error("[FallbackFareLoader] SRT CSV 파싱 실패: {}", e.getMessage());
        }
        return list;
    }

    private String cellStr(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default      -> "";
        };
    }

    // ── 기본값 ────────────────────────────────────────────────────────────────

    private List<FareDto> defaultKtxFares() {
        return List.of(
            fare("KTX","서울","부산",59800), fare("KTX","서울","동대구",39000),
            fare("KTX","서울","대전",23700), fare("KTX","서울","광주송정",46800)
        );
    }

    private List<FareDto> defaultMugunghwaFares() {
        return List.of(
            fare("무궁화호","서울","부산",28600), fare("무궁화호","서울","동대구",21200),
            fare("무궁화호","서울","대전",11300), fare("무궁화호","용산","목포",22500)
        );
    }

    private List<FareDto> defaultItxFares() {
        return List.of(
            fare("ITX-새마을","서울","부산",42600), fare("ITX-새마을","서울","동대구",28500),
            fare("ITX-새마을","서울","대전",15700)
        );
    }

    private List<FareDto> defaultSrtFares() {
        return List.of(
            fare("SRT","수서","부산",52600), fare("SRT","수서","동대구",34400),
            fare("SRT","수서","대전",20900), fare("SRT","수서","광주송정",46800)
        );
    }

    private FareDto fare(String type, String dep, String arr, long f) {
        return FareDto.builder().transportType(type).departureName(dep)
                .arrivalName(arr).seatClass("standard").fare(f).build();
    }
}
