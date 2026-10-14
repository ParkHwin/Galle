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
- **fallback 구조**: 열차는 API 장애 시 코레일 공시 시각표(엑셀)로 실제 운행 열차를 표시하고, 시각표도 없으면 mock 데이터 사용 (임의 출발시각 생성 없음)
- **소셜 로그인**: Google / Kakao / Naver OAuth2 로그인 → JWT 발급
- **즐겨찾기**: 로그인 사용자가 출발지·도착지 구간을 저장/삭제 (비로그인 시 프론트엔드 localStorage로 동작)

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
│       ├── security/      # OAuth2 로그인, JWT 발급/검증
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

## 소셜 로그인 / JWT 설정 방법

`application.yml`에 아래 설정을 추가합니다.

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: YOUR_GOOGLE_CLIENT_ID
            client-secret: YOUR_GOOGLE_CLIENT_SECRET
            scope: email, profile
          kakao:
            client-id: YOUR_KAKAO_CLIENT_ID
            client-secret: YOUR_KAKAO_CLIENT_SECRET
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            client-authentication-method: client_secret_post
            scope: profile_nickname, account_email
          naver:
            client-id: YOUR_NAVER_CLIENT_ID
            client-secret: YOUR_NAVER_CLIENT_SECRET
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope: email
        provider:
          kakao:
            authorization-uri: https://kauth.kakao.com/oauth/authorize
            token-uri: https://kauth.kakao.com/oauth/token
            user-info-uri: https://kapi.kakao.com/v2/user/me
            user-name-attribute: id
          naver:
            authorization-uri: https://nid.naver.com/oauth2.0/authorize
            token-uri: https://nid.naver.com/oauth2.0/token
            user-info-uri: https://openapi.naver.com/v1/nid/me
            user-name-attribute: response

jwt:
  secret: YOUR_JWT_SECRET_KEY          # 최소 32자 이상 랜덤 문자열
  expiration-ms: 86400000              # 토큰 만료 (ms), 기본 24시간
  redirect-uri: http://localhost:5173/oauth/callback  # 로그인 성공 후 프론트엔드로 리다이렉트할 주소
```

> 로그인 시작: `GET /oauth2/authorization/{google|kakao|naver}` → 로그인 성공 시 `jwt.redirect-uri?token=JWT`로 리다이렉트됩니다.
> 이후 API 요청은 `Authorization: Bearer {token}` 헤더로 인증합니다.

---

## fallback 구조와 데이터 파일

외부 API가 실패·타임아웃(10초)되면 교통수단별로 아래 순서로 대체합니다.

| 교통수단 | 1순위 | 2순위 (API 실패 시) | 3순위 |
|---|---|---|---|
| 열차 시간표 | 코레일 열차운행계획 API | 엑셀 시각표 (KTX·일반열차) + SRT 시각표 CSV | 시각표 파일을 하나도 못 읽으면 mock |
| 열차 운임 | 코레일 API는 운임 미제공 → 항상 운임표 파일 사용 | | |
| 고속버스 | TAGO 고속버스 API | 구간별 기준값 (`SearchService.BUS_FALLBACK`) | |
| 자가용 | 카카오 모빌리티 길찾기 API | 구간별 기준값 (`SearchService.CAR_FALLBACK`) | |

- 시각표에 없는 구간(예: 서울→제주)은 열차를 표시하지 않습니다. 출발시각을 임의로 만들지 않습니다.
- 시각표의 비고(매일·금토일·월화수목 등)로 검색 날짜의 운행 요일을 거릅니다. 공휴일·명절 특별수송은 반영하지 않습니다.
- `서울` 검색은 용산·청량리 출발 열차를, `광주` 검색은 광주송정 도착 열차를 함께 찾습니다. SRT는 수서 기준입니다.

서버 시작 시 아래 파일을 읽어 메모리에 캐싱합니다.

```
backend/src/main/resources/data/
├── KTX시간표.xlsx          # KTX 시각표 (수서 정차 열차는 SRT로 분류)
├── 일반열차시간표.xlsx      # ITX-새마을·ITX-마음·새마을·무궁화·누리로 시각표
├── srt_timetable.csv       # SRT 시각표 (docs/SRT 운행시각표('26.5.15.기준).hwp 에서 변환)
├── KTX운임표.xls           # KTX 운임
├── ITX운임표.xlsx          # ITX-새마을 운임
├── 무궁화호운임표.xls       # 무궁화호 운임
└── srt_fares.csv           # SRT 운임 (2020.01.14 기준)
```

> 이 폴더의 `*.xlsx`, `*.xls`, `*.csv`는 `.gitignore`에 포함되어 GitHub에 올라가지 않습니다. 원본은 `docs/`에 있으니 clone 후 `docs/`의 파일을 위 이름으로 복사하세요.
> 시각표 파싱 확인: `cd backend && ./gradlew test --tests "*TrainScheduleCacheTest"`

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
| GET | `/oauth2/authorization/{provider}` | 소셜 로그인 시작 (`google`/`kakao`/`naver`) |
| GET | `/api/auth/me` | 현재 로그인 사용자 정보 조회 (JWT 필요) |
| GET | `/api/favorites` | 즐겨찾기 목록 조회 (JWT 필요) |
| POST | `/api/favorites` | 즐겨찾기 추가 (JWT 필요) |
| DELETE | `/api/favorites?from=&to=` | 즐겨찾기 삭제 (JWT 필요) |

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
