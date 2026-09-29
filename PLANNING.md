# 갈래(Gallae) — 기획서

> 도시 간 교통수단 통합 비교 플랫폼
> "서울에서 부산, 어떻게 가는 게 제일 나을까? 10초 비교"

> **⚠️ 문서 상태 안내 (최종 갱신: 2026-07-02)**
> 1~5장(문제 정의, 솔루션, 경쟁 환경, 데이터 소스, MVP 스코프)은 초기 기획 의도를 그대로 담고 있어 지금도 유효하다.
> 다만 6장 "기술 설계"는 **초기 구상안(Next.js + Prisma + NextAuth + AWS RDS)**이며, 실제 구현은 이 구상과 다르게 **React + Vite / Spring Boot / Spring Security OAuth2 / MySQL·H2**로 진행됐다. 실제 아키텍처·API 스펙은 이 문서가 아니라 [`README.md`](./README.md)를 기준으로 봐야 한다. 6장은 "왜 이 스택으로 시작하려 했는지"에 대한 기록으로만 남겨두고, 실제 구현 스펙과 혼동하지 않도록 아래에 실제 구현 요약을 별도로 추가했다.

\---

## 1\. 문제 정의

서울에서 부산을 갈 때 선택지는 KTX, SRT, ITX, 무궁화호, 고속버스, 자가용, 항공까지 7가지다.
이 중 최적의 선택을 하려면 코레일 앱, SRT 앱, 고속버스 앱, 네이버 항공권, 네이버 지도를 **각각 열어서 따로 검색**해야 한다.

한 곳에서 가격·소요시간·출발시간을 비교할 수 있는 한국 서비스가 **0개**다.

\---

## 2\. 솔루션

출발지·도착지·날짜를 입력하면, 해당 구간의 모든 교통수단을 **가격·소요시간·출발시간** 기준으로 한 화면에 비교하고, 선택 시 해당 예매 사이트로 바로 연결한다.

\---

## 3\. 경쟁 환경

|서비스|범위|한계|
|-|-|-|
|네이버/카카오 지도|도시 내 이동 (버스+지하철+자동차)|도시 간 KTX·항공·고속버스 비교 불가|
|네이버 항공권|항공편 가격 비교|항공만. 철도·버스 미포함|
|코레일/SRT 앱|각각의 열차 예매|자사 열차만. 크로스 비교 없음|
|고속버스 티머니|고속버스 예매|버스만|
|Rome2rio|글로벌 멀티모달|한국 데이터 부정확, KTX/SRT 미구분, 한국어 미지원|

\---

## 4\. 데이터 소스

### 4-1. 정적 데이터 (운임표 → DB 적재)

|데이터|출처|형태|갱신 주기|
|-|-|-|-|
|KTX 운임|코레일 운임표 (xls)|19개 노선, 구간별 일반실/특실|연 1\~2회|
|SRT 운임|SRT 운임표 (xlsx, 2026.05.15)|8개 노선, 구간별 일반실/특실|연 1\~2회|
|ITX-새마을 운임|코레일 운임표|구간별 일반실/특실|연 1\~2회|
|무궁화호 운임|코레일 운임표|구간별 일반실|연 1\~2회|

### 4-2. API 데이터 (실시간 조회)

|API|출처|제공 정보|비고|
|-|-|-|-|
|한국철도공사\_열차운행정보|data.go.kr|KTX/ITX/무궁화 스케줄, 출발·도착 시간|3개월 전 \~ 1개월 후|
|국토교통부\_(TAGO)\_고속버스정보|data.go.kr|고속버스 배차, 요금, 터미널 정보|실시간|
|카카오 모빌리티 길찾기|developers.kakao.com|자가용 경로, 소요시간, 톨비, 거리|실시간|

### 4-3. 수동 구축

|데이터|방법|비고|
|-|-|-|
|SRT 시간표|SRT 사이트에서 주요 구간 정리 → JSON|경부선+호남선 메인 2개 노선 우선|
|평균 유류비 단가|오피넷 주유소 평균가 참조|주 1회 수동 갱신 또는 API 연동|

\---

## 5\. MVP 스코프

### 5-1. MVP에 포함 (Phase 1)

* **회원가입/로그인:** OAuth 소셜 로그인 (Google, Naver, Kakao)
* **검색:** 출발지 → 도착지 선택 (드롭다운)
* **비교 대상:** KTX, SRT, ITX-새마을, 무궁화호, 고속버스, 자가용
* **비교 항목:** 가격, 소요시간, 출발시간 (열차/버스)
* **정렬:** 가격순 / 시간순 / 출발시간순 토글
* **예매 연결:** 각 옵션에 해당 예매 사이트 딥링크
* **대상 구간:** 서울/수서/수도권 출발 → 부산, 대전, 대구, 광주, 제주(버스 제외) 5개 도시
* **검색 이력:** 로그인 사용자의 최근 검색 저장
* **면책 고지:** "실제 가격은 예매 사이트에서 확인하세요" 상시 표시

### 5-2. MVP에서 제외 (Phase 2 이후)

* 항공편 (스케줄+가격 API 확보 후)
* 전국 도시 간 전체 확장
* 가격 알림 / 즐겨찾기
* 날짜 선택 (MVP는 오늘/내일 고정 또는 날짜 입력)
* 모바일 앱 (PWA로 대체)

### 5-3. MVP 핵심 화면

```
\[화면 1: 메인 — 검색]
┌─────────────────────────────┐
│  갈래 — 어떻게 갈래?          │
│                             │
│  출발: \[서울 ▼]              │
│  도착: \[부산 ▼]              │
│  날짜: \[2026-06-20 ▼]       │
│                             │
│  \[비교하기]                   │
└─────────────────────────────┘

\[화면 2: 결과 — 비교]
┌─────────────────────────────────────────────────┐
│  서울 → 부산  |  가격순 · 시간순 · 출발순          │
│─────────────────────────────────────────────────│
│  🚄 KTX        59,800원   2시간 18분   \[예매] →  │
│  🚄 SRT        52,600원   2시간 15분   \[예매] →  │
│  🚄 ITX-새마을  42,600원   4시간 42분   \[예매] →  │
│  🚂 무궁화호    28,600원   5시간 30분   \[예매] →  │
│  🚌 고속버스    23,000원   4시간 20분   \[예매] →  │
│  🚗 자가용     \~58,000원   4시간 10분   \[상세] →  │
│─────────────────────────────────────────────────│
│  ⓘ 가격은 일반실 기준이며 실제 가격과 다를 수 있습니다  │
└─────────────────────────────────────────────────┘

\[화면 2-1: 자가용 상세]
┌─────────────────────────────┐
│  🚗 자가용 상세               │
│  거리: 325km                 │
│  예상 소요시간: 4시간 10분      │
│  고속도로 톨비: \~28,100원      │
│  예상 유류비: \~30,000원        │
│  합계: \~58,100원              │
│  ※ 중형차 연비 12km/L 기준     │
└─────────────────────────────┘
```

\---

## 6\. 기술 설계

### 6-0. 실제 구현 스택 (현재 · README.md 기준)

아래 6-1~6-6은 최초 기획 당시의 구상안이고, **실제로 채택되어 동작 중인 스택은 다음과 같다.**

|레이어|구상안 (6-1~6-6)|실제 구현|
|-|-|-|
|Frontend|Next.js (React) + Tailwind CSS|React 18 + Vite + JavaScript + React Router v6 + Axios (Tailwind 미사용, 커스텀 CSS 변수 기반)|
|Backend|Next.js API Routes|Spring Boot 3.3 (Java 17) — Spring Web, Spring Data JPA, WebFlux|
|Auth|NextAuth.js (Google/Naver/Kakao)|Spring Security OAuth2 Client + 자체 JWT 발급 (Google/Naver/Kakao 소셜 로그인은 동일하게 지원)|
|DB|MySQL 8.0 (AWS RDS)|MySQL 8.0 (운영) / H2 인메모리 (개발 fallback, `application.yml` 없을 때 자동 기동)|
|ORM|Prisma|Spring Data JPA (Hibernate), `ddl-auto: update`|
|배포|Vercel (FE+API) + AWS RDS|미확정 (별도 백엔드 서버 배포 필요 — Vercel 단일 배포 전제가 더 이상 성립하지 않음)|

구상안과 실제 구현이 갈라진 이유(별도 백엔드 서버로 전환)는 기록이 남아있지 않다. 이후 의사결정 시 참고할 수 있도록, 스택을 바꿀 때는 이 표를 함께 갱신할 것.

### 6-1. 아키텍처 (초기 구상안 — 미채택)

```
\[사용자] → \[Next.js 프론트엔드 (Vercel)]
                ↓
        \[Next.js API Routes / 백엔드]
          ↓        ↓          ↓          ↓
    \[AWS RDS]  \[공공 API]  \[카카오 API]  \[OAuth]
    (MySQL)    (스케줄)    (자가용 경로)  (Google/Naver/Kakao)
```

### 6-2. 인증 (초기 구상안 — 미채택, 실제로는 Spring Security OAuth2 + JWT로 구현됨)

|제공자|용도|연동 방식|
|-|-|-|
|Google|글로벌 사용자 커버|NextAuth.js Google Provider|
|Naver|한국 사용자 주력|NextAuth.js Custom Provider (Naver OAuth)|
|Kakao|한국 사용자 주력|NextAuth.js Custom Provider (Kakao OAuth)|

**인증 흐름:**

```
\[로그인 버튼 클릭] → \[OAuth 제공자 인증 화면]
    → \[인증 완료, 콜백]
    → \[DB에 사용자 생성/조회 (upsert)]
    → \[JWT 세션 발급]
    → \[로그인 완료]
```

**사전 등록 필요:**

* Google Cloud Console → OAuth 2.0 클라이언트 ID 생성
* Naver Developers → 애플리케이션 등록 → 네이버 로그인 API
* Kakao Developers → 애플리케이션 등록 → 카카오 로그인 API

### 6-3. 기술 스택 (초기 구상안 — 미채택, 실제 스택은 위 6-0 참고)

|레이어|기술|이유|
|-|-|-|
|Frontend|Next.js (React) + Tailwind CSS|SSR로 SEO 확보, 빠른 UI 개발|
|Backend|Next.js API Routes|별도 서버 불필요, Vercel 배포 일체화|
|Auth|NextAuth.js|Google/Naver/Kakao OAuth 통합, JWT 세션|
|DB|MySQL 8.0 (AWS RDS)|안정성, AWS 프리 티어 (db.t3.micro 12개월 무료)|
|ORM|Prisma|MySQL 지원, 타입 안전, 마이그레이션 관리|
|배포|Vercel (FE+API) + AWS RDS (DB)|Vercel 무료, RDS 프리 티어|
|API 통신|axios|다중 API 병렬 호출|

### 6-4. DB 스키마 (초기 구상안 — 미채택, 실제 스키마는 6-4-1 참고)

```sql
-- ============================================
-- 인증 관련 (NextAuth.js 표준 스키마)
-- ============================================

-- 사용자
CREATE TABLE users (
    id              VARCHAR(191) PRIMARY KEY,
    name            VARCHAR(100),
    email           VARCHAR(255) UNIQUE,
    email\_verified  TIMESTAMP NULL,
    image           VARCHAR(500),               -- 프로필 이미지 URL
    created\_at      TIMESTAMP DEFAULT CURRENT\_TIMESTAMP,
    updated\_at      TIMESTAMP DEFAULT CURRENT\_TIMESTAMP ON UPDATE CURRENT\_TIMESTAMP
);

-- OAuth 계정 연결 (1 user : N accounts)
CREATE TABLE accounts (
    id                  VARCHAR(191) PRIMARY KEY,
    user\_id             VARCHAR(191) NOT NULL,
    type                VARCHAR(50) NOT NULL,       -- 'oauth'
    provider            VARCHAR(50) NOT NULL,       -- 'google', 'naver', 'kakao'
    provider\_account\_id VARCHAR(191) NOT NULL,
    access\_token        TEXT,
    refresh\_token       TEXT,
    expires\_at          INT,
    token\_type          VARCHAR(50),
    scope               VARCHAR(500),
    UNIQUE(provider, provider\_account\_id),
    FOREIGN KEY (user\_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 세션
CREATE TABLE sessions (
    id            VARCHAR(191) PRIMARY KEY,
    session\_token VARCHAR(255) UNIQUE NOT NULL,
    user\_id       VARCHAR(191) NOT NULL,
    expires       TIMESTAMP NOT NULL,
    FOREIGN KEY (user\_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================
-- 서비스 데이터
-- ============================================

-- 역/터미널 마스터
CREATE TABLE stations (
    id          INT AUTO\_INCREMENT PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,           -- '서울', '부산', '동대구'
    type        VARCHAR(20) NOT NULL,           -- 'ktx', 'srt', 'bus\_terminal'
    city        VARCHAR(30) NOT NULL,           -- 소속 도시 (검색 매핑용)
    code        VARCHAR(20),                    -- API용 역코드
    booking\_url VARCHAR(500)                    -- 예매 딥링크 템플릿
);

-- 운임 테이블 (정적 데이터)
CREATE TABLE fares (
    id              INT AUTO\_INCREMENT PRIMARY KEY,
    transport\_type  VARCHAR(20) NOT NULL,       -- 'ktx', 'srt', 'itx', 'mugunghwa'
    departure\_id    INT NOT NULL,
    arrival\_id      INT NOT NULL,
    class           VARCHAR(10) DEFAULT 'standard', -- 'standard', 'first'
    fare            INT NOT NULL,               -- 원 단위
    updated\_at      TIMESTAMP DEFAULT CURRENT\_TIMESTAMP ON UPDATE CURRENT\_TIMESTAMP,
    UNIQUE(transport\_type, departure\_id, arrival\_id, class),
    FOREIGN KEY (departure\_id) REFERENCES stations(id),
    FOREIGN KEY (arrival\_id) REFERENCES stations(id)
);

-- 도시 매핑 (검색용)
CREATE TABLE cities (
    id      INT AUTO\_INCREMENT PRIMARY KEY,
    name    VARCHAR(30) NOT NULL UNIQUE,        -- '서울', '부산'
    region  VARCHAR(30)                         -- '수도권', '영남권'
);

-- 도시-역 매핑 (1:N)
CREATE TABLE city\_stations (
    city\_id     INT NOT NULL,
    station\_id  INT NOT NULL,
    is\_primary  BOOLEAN DEFAULT false,          -- 대표역 여부
    PRIMARY KEY (city\_id, station\_id),
    FOREIGN KEY (city\_id) REFERENCES cities(id),
    FOREIGN KEY (station\_id) REFERENCES stations(id)
);

-- 검색 로그 (로그인 사용자는 user\_id 기록)
CREATE TABLE search\_logs (
    id              INT AUTO\_INCREMENT PRIMARY KEY,
    user\_id         VARCHAR(191),               -- NULL이면 비로그인 검색
    departure\_city  VARCHAR(30),
    arrival\_city    VARCHAR(30),
    search\_date     DATE,                       -- 검색한 여행 날짜
    searched\_at     TIMESTAMP DEFAULT CURRENT\_TIMESTAMP,
    FOREIGN KEY (user\_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx\_user\_searched (user\_id, searched\_at)
);
```

### 6-4-1. 실제 DB 스키마 (JPA 엔티티 기준, 현재)

위 6-4는 채택되지 않았다. 실제로는 `id`가 문자열(VARCHAR)이 아니라 `BIGINT AUTO_INCREMENT`이고, NextAuth 표준 테이블(`accounts`, `sessions`) 대신 자체 JWT 방식을 쓰기 때문에 구조가 더 단순하다.

|테이블|주요 컬럼|비고|
|-|-|-|
|`users`|id, provider, provider\_id, email, nickname, profile\_image\_url, role, created\_at, updated\_at|`(provider, provider_id)` 유니크 — accounts 테이블 없이 소셜 계정 1:1|
|`stations`|id, name, type, city, code, booking\_url|기획안의 stations와 거의 동일|
|`fares`|id, transport\_type, departure\_id(FK→stations), arrival\_id(FK→stations), class\_type, fare|`(transport_type, departure_id, arrival_id, class_type)` 유니크|
|`search_logs`|id, departure\_city, arrival\_city, search\_date, searched\_at|**user\_id 컬럼이 없다** — 로그인 사용자와 연결되지 않은 상태로 남아 있음 (아래 참고)|

> **알려진 갭:** `search_logs`에 로그인 사용자를 연결하는 컬럼이 없어서, 기획 의도("로그인 사용자의 최근 검색 저장")가 실제로는 구현되어 있지 않다. 즐겨찾기 기능도 서버 저장이 아니라 프론트엔드 `localStorage`로만 동작한다 (2026-07-02 기준: 로그인 사용자에 한해 서버 저장으로 전환 작업 진행, 아래 `favorites` 테이블 추가 참고).
>
> |테이블|주요 컬럼|비고|
> |-|-|-|
> |`favorites`|id, user\_id(FK→users), departure\_city, arrival\_city, label, created\_at|로그인 사용자 전용. 비로그인 사용자는 기존과 동일하게 `localStorage`만 사용|

**AWS RDS 설정:**

* 엔진: MySQL 8.0
* 인스턴스: db.t3.micro (프리 티어, 12개월 무료)
* 스토리지: 20GB gp2
* 리전: ap-northeast-2 (서울)
* 퍼블릭 액세스: 개발 중 Yes → 배포 후 Vercel IP만 허용

### 6-5. API 설계 (초기 구상안 — 미채택, 실제 엔드포인트는 README.md "주요 API 명세" 참고)

실제 통합 검색 엔드포인트는 `GET /api/search/routes` (아래 `/api/compare` 아님)이며, 응답 필드명도 snake_case가 아니라 camelCase(`departureName`, `durationMinutes` 등)로 구현되어 있다. 아래 예시는 초기 구상 당시의 스펙이다.

```
GET /api/compare
  ?from=서울
  \&to=부산
  \&date=2026-06-20

Response:
{
  "departure": "서울",
  "arrival": "부산",
  "date": "2026-06-20",
  "results": \[
    {
      "type": "ktx",
      "label": "KTX",
      "fare": 59800,
      "class": "일반실",
      "duration\_minutes": 138,
      "schedules": \[
        { "departure": "05:15", "arrival": "07:33", "train\_no": "KTX-101" },
        { "departure": "05:40", "arrival": "08:02", "train\_no": "KTX-103" }
      ],
      "booking\_url": "https://www.letskorail.com/...",
      "source": "static"
    },
    {
      "type": "srt",
      "label": "SRT",
      "fare": 52600,
      "class": "일반실",
      "duration\_minutes": 135,
      "schedules": \[...],
      "booking\_url": "https://etk.srail.kr/...",
      "source": "static"
    },
    {
      "type": "express\_bus",
      "label": "고속버스",
      "fare": 23000,
      "duration\_minutes": 260,
      "schedules": \[...],
      "booking\_url": "https://www.kobus.co.kr/...",
      "source": "tago\_api"
    },
    {
      "type": "car",
      "label": "자가용",
      "fare": 58100,
      "duration\_minutes": 250,
      "distance\_km": 325,
      "toll": 28100,
      "fuel\_cost": 30000,
      "fuel\_standard": "중형차 12km/L 기준",
      "source": "kakao\_api"
    }
  ],
  "disclaimer": "표시된 가격은 참고용이며 실제 가격과 다를 수 있습니다."
}
```

### 6-6. 데이터 흐름 (초기 구상안 — 개념은 유지, 세부 구현은 SearchService/TrainScheduleCache 참고)

```
\[검색 요청] from=서울, to=부산, date=2026-06-20
    │
    ├─ \[1] DB 조회 (운임) ─── KTX/SRT/ITX/무궁화 가격 즉시 반환
    │
    ├─ \[2] 코레일 API 호출 ── KTX/ITX/무궁화 스케줄 (날짜 기준)
    │       └─ 캐시 히트 시 캐시 반환 (TTL: 6시간)
    │
    ├─ \[3] SRT 스케줄 ─────── 정적 JSON에서 조회
    │
    ├─ \[4] TAGO API 호출 ──── 고속버스 스케줄+요금
    │       └─ 캐시 히트 시 캐시 반환 (TTL: 6시간)
    │
    ├─ \[5] 카카오 API 호출 ── 자가용 경로+톨비+거리
    │       └─ 캐시 히트 시 캐시 반환 (TTL: 24시간)
    │
    └─ \[6] 응답 조합 ────── 모든 결과를 통합 정렬하여 반환
```

\---

## 7\. 개발 일정 (MVP)

|주차|작업|산출물|
|-|-|-|
|**1주차**|프로젝트 세팅, AWS RDS 생성, DB 스키마 생성 (Prisma), 운임 데이터 파싱·적재 스크립트|DB에 KTX/SRT/ITX/무궁화 운임 적재 완료|
|**2주차**|NextAuth.js 설정, Google/Naver/Kakao OAuth 연동, 로그인/회원가입 UI|소셜 로그인 동작 완료|
|**3주차**|코레일 열차운행정보 API 연동, TAGO 고속버스 API 연동|스케줄 데이터 조회 및 정규화|
|**4주차**|카카오 지도 API 연동 (자가용), SRT 시간표 정적 데이터 구축|전 교통수단 데이터 파이프라인 완성|
|**5주차**|비교 API 개발 (`/api/compare`), 캐싱 레이어, 검색 이력 저장|백엔드 완성|
|**6주차**|프론트엔드 — 검색 화면, 결과 비교 화면, 마이페이지 (검색 이력)|UI 1차 완성|
|**7주차**|자가용 상세, 예매 딥링크, 면책 고지, 반응형|UI 완성|
|**8주차**|테스트, 버그 수정, SEO 메타태그, 배포|Vercel 배포 완료|
|**9주차**|사용자 테스트 (10명), 피드백 반영, SEO 블로그 1편|MVP 런칭|

\---

## 8\. 수익 모델

### Phase 1 — 어필리에이트 (Day 1)

* 예매 버튼 클릭 → 코레일/SRT/고속버스 예매 사이트 연결
* 항공 추가 시 스카이스캐너/네이버 항공권 어필리에이트
* 전환당 수수료 또는 CPC

### Phase 2 — 프리미엄 (사용자 확보 후)

* 월 1,900원: 가격 알림, 즐겨찾기, 검색 이력, 주간 리포트
* 타겟: 주 1회 이상 도시 간 이동하는 출장 직장인

### Phase 3 — 광고 (트래픽 확보 후)

* 검색 결과 페이지 내 렌터카/숙소 광고
* 일일 1,000+ 방문자 이후

\---

## 9\. GTM (Go-to-Market)

### 핵심 채널 (우선순위)

1. **SEO** — "서울 부산 KTX 가격", "서울 대전 교통비 비교" 등 기존 검색 수요 흡수
2. **에브리타임** — 대학생 귀성길 시즌 (설/추석/방학) 타이밍
3. **디스콰이엇** — 사이드 프로젝트 커뮤니티 노출
4. **시즌 마케팅** — 추석/설 2주 전 배포, "귀성길 교통 비교" 프레이밍

### 타겟 사용자

|타겟|사용 빈도|핵심 니즈|
|-|-|-|
|대학생 귀성길|연 4\~8회|KTX 매진 시 대안, 최저가|
|출장 직장인|주 1\~2회|가장 빠른 옵션, 시간대별 비교|
|주말 여행자|월 1\~2회|가격·편의성 균형|

\---

## 10\. 리스크 및 대응

|리스크|심각도|대응|
|-|-|-|
|공공 API 응답 지연/장애|높음|6시간 TTL 캐싱, 정적 데이터 폴백|
|운임 변경 시 정적 데이터 갱신 지연|중간|"마지막 업데이트: YYYY-MM-DD" 표시, 분기 1회 점검|
|SRT 스케줄 API 부재|중간|수동 시간표 JSON, 노선 2개라 관리 가능|
|네이버/카카오가 동일 기능 출시|높음|선점 + SEO 확보로 전환 비용 생성|
|항공권 실시간 가격 확보 불가|낮음|"가격대 범위 + 예매 딥링크"로 우회|

\---

## 11\. 성공 지표 (MVP 런칭 후 1개월)

|지표|목표|
|-|-|
|주간 검색 수|100회+|
|예매 링크 클릭률|30%+|
|SEO 유입 비율|50%+|
|"다시 사용할 의향" (테스트 그룹)|8/10명+|



