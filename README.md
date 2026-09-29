# 갈래(Gallae) — 도시 간 교통수단 통합 비교 플랫폼

> "서울에서 부산, 어떻게 갈래? 10초 비교"

출발지·도착지·날짜를 입력하면 KTX, SRT, ITX, 새마을호, 고속버스, 자가용의 가격·소요시간·출발시간을 한 화면에서 비교하고 예매 사이트로 바로 연결합니다.

---

## 주요 기능

- **통합 검색**: 출발지/도착지/날짜/시간 입력 → 전 교통수단 한 번에 비교
- **비교 항목**: 예상 운임, 소요시간, 출발/도착 시간
- **추천 시스템**: 최저가 / 최단시간 / 종합 추천 자동 표시
- **교통수단 필터**: KTX, SRT, ITX-마음, ITX-새마을, 새마을호, 고속버스, 자가용
- **예매 연결**: 각 결과에서 해당 예매 사이트로 직접 이동
- **fallback 구조**: API 장애 시 CSV/XLS 운임 데이터 또는 mock 데이터 자동 사용

---

## 기술 스택

| 레이어 | 기술 |
|---|---|
| Frontend | React 18, Vite, JavaScript, React Router v6, Axios |
| Backend | Spring Boot 3.3, Java 17, Spring Web, Spring Data JPA, WebFlux |
| DB | MySQL 8.0 (개발: H2 인메모리) |
| 외부 API | 코레일 열차운행정보, TAGO 고속버스, 카카오 모빌리티, SRT 여객운임 |

---

## 폴더 구조

```
갈래_Gallae/
├── frontend/              # React + Vite SPA
│   ├── src/
│   │   ├── api/           # Axios 인스턴스 및 API 함수
│   │   ├── components/    # 재사용 컴포넌트
│   │   ├── pages/         # 라우트별 페이지
│   │   └── styles/        # 글로벌 CSS 변수
│   ├── .env.example
│   └── package.json
│
├── backend/               # Spring Boot API 서버
│   └── src/main/java/com/gallae/
│       ├── config/        # CORS, WebClient, 외부 API 설정
│       ├── controller/    # REST 엔드포인트
│       ├── service/       # 비즈니스 로직
│       ├── client/        # 외부 API 클라이언트
│       ├── dto/           # 요청/응답 DTO
│       ├── entity/        # JPA 엔티티
│       ├── repository/    # DB 접근
│       ├── exception/     # 전역 예외 처리
│       └── util/          # Mock·Fallback 데이터
│
├── docs/                  # 기획·설계 문서
├── .gitignore
└── README.md
```

---

## 프론트엔드 실행 방법

```bash
cd frontend
npm install
npm run dev
# http://localhost:5173
```

> API 서버가 없어도 mock 데이터로 검색 결과가 표시됩니다.

---

## 백엔드 실행 방법

### 1. application.yml 설정

`application-example.yml`을 복사해 `application.yml`로 만들고 값을 채웁니다.

```bash
cd backend/src/main/resources
cp application-example.yml application.yml
```

```yaml
# application.yml (MySQL 사용 시)
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/gallae?serverTimezone=Asia/Seoul
    username: root
    password: YOUR_DB_PASSWORD
```

> `application.yml`이 없으면 H2 인메모리 DB로 자동 기동됩니다.

### 2. 서버 기동

```bash
cd backend
./gradlew bootRun
# http://localhost:8080
```

### 3. 동작 확인

```bash
curl http://localhost:8080/api/health
curl "http://localhost:8080/api/search/routes?from=서울&to=부산&date=2026-06-19&time=09:00"
```

---

## MySQL 설정 방법

```sql
CREATE DATABASE gallae
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

`application.yml`의 datasource 설정에 위 DB 정보를 입력하면 JPA가 테이블을 자동 생성합니다 (`ddl-auto: update`).

---

## 외부 API 키 설정 방법

`backend/src/main/resources/application.yml`에 아래 키를 추가합니다.

```yaml
external:
  srt:
    service-key: YOUR_SRT_SERVICE_KEY    # https://www.data.go.kr

  korail:
    service-key: YOUR_KORAIL_SERVICE_KEY  # https://www.data.go.kr

  bus:
    service-key: YOUR_BUS_SERVICE_KEY     # https://www.data.go.kr

  kakao:
    api-key: YOUR_KAKAO_MOBILITY_API_KEY  # https://developers.kakao.com
```

> 키가 없어도 서버는 기동되며, 해당 API 실패 시 fallback 데이터를 자동으로 사용합니다.

---

## fallback 운임 파일 위치

API 실패 시 아래 파일을 자동으로 참조합니다. 파일을 해당 경로에 복사해두면 더 정확한 데이터를 사용할 수 있습니다.

```
backend/src/main/resources/data/
├── (주)에스알_srt여객운임 정보_20200114.csv    # SRT 운임 fallback
├── KTX운임표.xls                              # KTX 운임 fallback
└── 일반열차(ITX-마음, ITX-새마을, 새마을호) 운임표(2025.12.30.부터.xlsx
```

> 이 파일들은 `.gitignore`에 포함되어 있어 GitHub에 올라가지 않습니다.

---

## 주요 API 명세

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/health` | 서버 상태 확인 |
| GET | `/api/search/routes` | 통합 교통수단 검색 |
| GET | `/api/fares/srt` | SRT 운임 조회 |
| GET | `/api/fares/ktx` | KTX 운임 조회 |
| GET | `/api/fares/normal-train` | 일반열차 운임 조회 |
| GET | `/api/train/codes` | 열차 코드 조회 |
| GET | `/api/train/plans` | 열차 운행계획 조회 |
| GET | `/api/train/runs` | 열차 운행정보 조회 |
| GET | `/api/bus/cities` | 고속버스 도시 코드 조회 |
| GET | `/api/bus/terminals` | 터미널 조회 |
| GET | `/api/bus/grades` | 버스 등급 조회 |
| GET | `/api/bus/routes` | 고속버스 노선 조회 |
| GET | `/api/car/directions` | 자가용 길찾기 |
| GET | `/api/auth/me` | 현재 로그인 사용자 정보 조회 (JWT 필요) |

### 통합 검색 예시

```
GET /api/search/routes?from=서울&to=부산&date=2026-06-19&time=09:00
```

```json
{
  "success": true,
  "data": {
    "from": "서울",
    "to": "부산",
    "date": "2026-06-19",
    "time": "09:00",
    "results": [
      {
        "type": "KTX",
        "name": "KTX 101",
        "departureName": "서울역",
        "arrivalName": "부산역",
        "departureTime": "09:00",
        "arrivalTime": "11:18",
        "durationMinutes": 138,
        "fare": 59800,
        "recommendTags": ["최단시간", "종합추천"],
        "bookingUrl": "https://www.letskorail.com"
      }
    ],
    "summary": {
      "cheapest": "고속버스",
      "fastest": "KTX",
      "recommended": "KTX"
    },
    "disclaimer": "표시된 가격은 참고용이며 실제 가격은 각 예매 사이트에서 확인하세요."
  }
}
```

---

## GitHub 업로드 시 주의사항

아래 파일들은 `.gitignore`에 포함되어 **GitHub에 올라가지 않습니다.**

- `backend/src/main/resources/application.yml` — DB 비밀번호, API 키 포함
- `frontend/.env`, `frontend/.env.local` — 환경변수
- `backend/src/main/resources/data/*.csv`, `*.xls`, `*.xlsx` — 운임 데이터 파일
- `.idea/`, `.vscode/` — IDE 설정

커밋 전 반드시 확인:
```bash
git status
git diff --cached
```

예시 파일은 커밋 가능합니다:
- `application-example.yml` ✅
- `.env.example` ✅

---

## 라이선스

개인 사이드 프로젝트. 교통 운임 데이터는 공공데이터포털 및 코레일/SRT 공식 자료를 기반으로 합니다.
