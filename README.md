# DeviceHub

## React 관리자 웹 진행 상태

React, Vite, JavaScript, axios, CSS Modules로 DeviceHub 관리자 웹을 구성했다. 별도 UI 라이브러리와 전역 상태 관리 도구는 사용하지 않고 `useState`, `useEffect`를 사용한다.

현재 실제 기능이 연결된 메뉴는 `Devices`이며 Dashboard, Apps, Users, Settings는 향후 기능을 위한 화면 구조만 제공한다.

### 관리자 웹 실행

터미널 1에서 Spring Boot 백엔드를 실행한다.

```powershell
.\gradlew.bat bootRun
```

터미널 2에서 프론트엔드를 실행한다.

```powershell
cd frontend
npm install
npm run dev
```

브라우저에서 <http://localhost:5173>으로 접속한다. Vite 개발 서버는 `/api` 요청을 `http://localhost:8080`으로 전달하므로 기존 백엔드 API path를 변경하거나 별도 CORS 설정을 추가하지 않는다.

Production build 확인:

```powershell
cd frontend
npm run build
```

### Devices 관리자 기능

- 이름, 타입, 제조사, 모델명, OS 버전을 대상으로 한 클라이언트 검색
- 기기 목록과 PHONE/TABLET 상태 배지
- 우측 Drawer를 이용한 기기 등록, 상세 조회, 수정
- 별도 확인 Dialog를 이용한 기기 삭제
- 로딩, API 오류, 빈 목록, 검색 결과 없음 상태
- axios를 통한 기존 `/api/devices` CRUD 연동

프론트엔드 구조:

```text
frontend/src/
├─ api/                  # axios 인스턴스와 Device API 호출
├─ components/common/    # 공통 Icon, 준비 중 화면
├─ components/layout/    # Sidebar, Header, Main 레이아웃
├─ features/devices/     # Devices 화면, Table, Drawer, Delete Dialog
└─ styles/               # 전역 디자인 토큰과 기본 스타일
```

## Phase 3.5 진행 상태 (2026-09-01)

Swagger/OpenAPI 문서화를 추가해 브라우저에서 현재 Health API와 Device CRUD API를 확인하고 직접 호출할 수 있다. Swagger는 API 문서를 다루는 도구 생태계이고, OpenAPI는 API 구조를 표현하는 표준 명세다. 이 프로젝트에서는 `springdoc-openapi`가 Spring MVC Controller와 DTO를 분석해 OpenAPI 문서와 Swagger UI를 자동 생성한다.

### Swagger 실행 및 접속

PostgreSQL 서비스를 실행한 상태에서 Spring Boot를 시작한다.

```powershell
.\gradlew.bat bootRun
```

- Swagger UI: <http://localhost:8080/swagger-ui/index.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

Swagger UI의 `Devices` 태그를 펼치면 다음 API를 확인할 수 있다.

- `POST /api/devices`
- `GET /api/devices`
- `GET /api/devices/{id}`
- `PUT /api/devices/{id}`
- `DELETE /api/devices/{id}`

API를 직접 시험하려면 원하는 API를 펼친 뒤 `Try it out`을 누르고 요청 본문이나 `id`를 입력한 다음 `Execute`를 누른다. `Execute`는 예시 화면만 바꾸는 것이 아니라 브라우저에서 실행 중인 DeviceHub 서버로 실제 HTTP 요청을 전송하므로 PostgreSQL 데이터도 실제로 생성·수정·삭제된다.

`Schemas` 영역에서는 `DeviceCreateRequest`, `DeviceUpdateRequest`, `DeviceResponse` 구조를 볼 수 있다. `type`은 `PHONE`, `TABLET` 중 하나이며, 필수 요청 필드와 각 API의 200, 201, 204, 400, 404 응답도 문서에 표시된다. 기존 `GET /api/health`도 Swagger UI에 자동 노출된다.

각 API를 펼치면 기능 설명, 성공·오류 응답의 의미, `id` 파라미터 설명을 볼 수 있다. DTO Schema에는 필드별 설명과 입력 예시가 표시되므로 `Try it out`에서 예시를 참고해 요청을 작성할 수 있다. Health API는 `Health` 태그로 구분된다.

OpenAPI 기본 정보:

```text
title: DeviceHub API
description: DeviceHub device management API
version: v1
```

현재 Swagger에는 인증이 없다. 향후 JWT가 추가되면 OpenAPI Security Scheme을 정의하고 Swagger UI의 `Authorize` 버튼에 Bearer token을 입력하는 방식을 사용할 수 있다. Phase 3.5에서는 Spring Security와 JWT를 추가하지 않았다.

## Phase 3 진행 상태 (2026-09-01)

Device CRUD API와 PostgreSQL 영속화 구현을 완료했다. Phase 4의 User, 로그인, JWT는 아직 구현하지 않았다.

### Device 모델

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | `Long` | PostgreSQL Identity로 자동 생성되는 PK |
| `name` | `String` | 필수 기기 이름 |
| `type` | `DeviceType` | 필수, `PHONE` 또는 `TABLET` |
| `manufacturer` | `String` | 필수 제조사 |
| `modelName` | `String` | 필수 모델명 |
| `osVersion` | `String?` | 선택 OS 버전 |
| `createdAt` | `LocalDateTime` | 생성 시각 |
| `updatedAt` | `LocalDateTime` | 마지막 수정 시각 |

Entity는 DB 매핑에만 사용하고 API에는 `DeviceCreateRequest`, `DeviceUpdateRequest`, `DeviceResponse` DTO를 사용한다.

### Device API

| Method | Path | 성공 상태 | 기능 |
|---|---|---:|---|
| `POST` | `/api/devices` | 201 | Device 생성 |
| `GET` | `/api/devices` | 200 | 전체 조회 |
| `GET` | `/api/devices/{id}` | 200 | 단건 조회 |
| `PUT` | `/api/devices/{id}` | 200 | 전체 필드 수정 |
| `DELETE` | `/api/devices/{id}` | 204 | 삭제 |

존재하지 않는 ID의 단건 조회·수정·삭제는 404를 반환한다. `name`, `manufacturer`, `modelName`이 공백이거나 `type`이 없으면 400을 반환한다.

생성 요청 예시:

```json
{
  "name": "개발용 갤럭시",
  "type": "PHONE",
  "manufacturer": "Samsung",
  "modelName": "Galaxy S25+",
  "osVersion": "Android 16"
}
```

응답 예시:

```json
{
  "id": 1,
  "name": "개발용 갤럭시",
  "type": "PHONE",
  "manufacturer": "Samsung",
  "modelName": "Galaxy S25+",
  "osVersion": "Android 16",
  "createdAt": "2026-09-01T10:54:29.133456",
  "updatedAt": "2026-09-01T10:54:29.133456"
}
```

### 스키마 관리

Phase 3에서는 별도 migration 도구를 추가하지 않고 `src/main/resources/schema.sql`을 사용한다. `spring.sql.init.mode=always`가 애플리케이션 시작 시 SQL을 실행하며, `CREATE TABLE IF NOT EXISTS`이므로 기존 테이블과 데이터를 삭제하지 않는다. `ddl-auto`는 계속 `none`으로 유지해 Hibernate가 스키마를 임의 변경하지 않게 한다.

### 실행 및 확인

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat bootRun
```

환경변수를 지정하지 않으면 `localhost:5432/devicehub`, 사용자 `devicehub`, 암호 `devicehub`를 사용한다. 다른 환경에서는 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 지정한다.

PostgreSQL에서 직접 확인:

```powershell
$env:PGPASSWORD = "devicehub"
& "C:\Program Files\PostgreSQL\17\bin\psql.exe" -h localhost -p 5432 -U devicehub -d devicehub -c "SELECT * FROM device;"
Remove-Item Env:PGPASSWORD
```

### Phase 3 학습 내용

- `@Entity`는 클래스를 JPA가 관리하는 DB 매핑 대상으로 만들고, `@Id`는 PK를, `@GeneratedValue`는 PK 자동 생성 전략을 지정한다.
- `@Enumerated(EnumType.STRING)`은 enum 순서 숫자가 아니라 `PHONE`, `TABLET` 문자열을 저장해 enum 순서 변경에 안전하고 DB 값을 읽기 쉽게 한다.
- DTO 분리는 DB 구조와 외부 API 계약을 분리해 Entity 내부 변경이나 불필요한 필드 노출이 API에 번지는 것을 막는다.
- Repository는 DB 접근을 담당한다. `JpaRepository`는 `save()`, `findById()`, `findAll()`, `delete()` 같은 기본 CRUD를 구현해 제공한다.
- Service는 트랜잭션, 조회 실패 처리, Entity 변경, Entity와 DTO 변환 같은 애플리케이션 로직을 담당한다.
- Controller는 HTTP 요청을 받아 검증한 뒤 Service를 호출하고 상태 코드와 응답을 돌려준다. 비즈니스 로직은 넣지 않는다.
- `@Valid`는 Request DTO의 `@NotBlank`, `@NotNull` 규칙을 실행하며 실패하면 요청을 Service에 전달하지 않고 400을 반환한다.
- 처리 흐름은 `HTTP 요청 → Controller → Service → Repository → JPA/Hibernate → JDBC Driver → PostgreSQL`이며 응답은 반대 방향으로 돌아온다.
- `POST`는 생성, `GET`은 조회, `PUT`은 지정 자원의 전체 수정, `DELETE`는 삭제에 사용한다.
- 200은 정상 조회·수정, 201은 생성 완료, 204는 응답 본문 없는 성공, 400은 잘못된 요청, 404는 자원을 찾지 못했다는 뜻이다.

## Phase 2 진행 상태 (2026-09-01)

- PostgreSQL 17.11 database cluster 초기화 및 Windows 서비스 등록 완료
- 개발용 `devicehub` 사용자와 `devicehub` database 생성 완료
- Spring Data JPA와 PostgreSQL JDBC Driver 추가 완료
- datasource는 환경변수 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 지원하며 로컬 개발 기본값을 제공
- 학습 단계에서 의도하지 않은 테이블 변경을 막기 위해 `spring.jpa.hibernate.ddl-auto`는 `none`으로 설정
- Gradle 테스트 및 빌드 성공, Spring Boot에서 PostgreSQL 17.11 연결 확인
- 실제 `GET /api/health` 호출에서 HTTP 200과 `{"status":"UP"}` 응답 확인

### Phase 2 핵심 개념

- Spring Data JPA는 Kotlin 객체와 관계형 데이터베이스 사이의 데이터 접근 계층을 만드는 기능을 제공한다.
- PostgreSQL JDBC Driver는 Spring Boot 애플리케이션이 JDBC 프로토콜로 PostgreSQL에 접속하게 한다.
- `spring.datasource` 설정을 읽은 Spring Boot는 `DataSource`와 커넥션 풀을 자동 구성한다.
- `${DB_URL:기본값}` 형식은 환경변수가 있으면 그 값을 사용하고, 없으면 콜론 뒤의 로컬 기본값을 사용한다.
- Phase 3의 Entity와 Device CRUD는 아직 구현하지 않았다.

개인 기기를 관리하기 위해 Kotlin과 Spring Boot로 만드는 학습용 백엔드 서버입니다.

## 현재 진행 상태

- Phase 1 완료: Spring Boot 프로젝트 구성 및 Health API
- Phase 2 완료: PostgreSQL 연결, Spring Data JPA 및 JDBC Driver 구성
- 다음 단계(미진행): Phase 3 Device CRUD

현재는 Spring Web, Spring Data JPA, PostgreSQL JDBC Driver를 사용합니다. Security, JWT, Swagger/OpenAPI, Docker는 아직 추가하지 않았습니다.

## 기술 환경

- Java 21
- Kotlin 1.9.25
- Spring Boot 3.4.5
- Gradle Kotlin DSL
- Spring Web
- Spring Data JPA
- PostgreSQL 17.11

## 실행 방법

먼저 `java -version`으로 Java 21이 설치되어 있고 `JAVA_HOME`이 Java 21을 가리키는지 확인합니다.

Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

macOS/Linux:

```shell
./gradlew bootRun
```

서버는 기본적으로 `http://localhost:8080`에서 실행됩니다. 다른 터미널에서 다음 요청으로 확인할 수 있습니다.

```shell
curl http://localhost:8080/api/health
```

응답:

```json
{
  "status": "UP"
}
```

## 테스트 및 빌드

Windows에서는 다음 명령을 실행합니다.

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

`HealthControllerTest`는 애플리케이션 컨텍스트를 시작하고 MockMvc로 `GET /api/health`를 호출하여 HTTP 200, JSON Content-Type, `status` 값이 `UP`인지 검증합니다.

## 폴더 구조

```text
device-hub/
├─ gradle/wrapper/                    # Gradle Wrapper 설정과 실행 파일
├─ src/
│  ├─ main/
│  │  ├─ kotlin/com/devicehub/
│  │  │  ├─ DeviceHubApplication.kt  # 애플리케이션 시작점
│  │  │  └─ health/
│  │  │     └─ HealthController.kt   # Health API와 응답 모델
│  │  └─ resources/
│  │     └─ application.yml          # 애플리케이션 설정
│  └─ test/kotlin/com/devicehub/health/
│     └─ HealthControllerTest.kt      # Health API 통합 테스트
├─ build.gradle.kts                   # 플러그인, 의존성, Java 버전, 빌드 설정
├─ settings.gradle.kts                # Gradle 프로젝트 이름
├─ gradlew / gradlew.bat              # OS별 Gradle Wrapper 실행 스크립트
├─ AGENTS.md                           # 이 프로젝트의 협업 및 학습 규칙
└─ README.md                           # 실행 방법과 현재 학습 진행 상태
```

## Phase 1 학습 내용

### 애플리케이션 시작 과정

1. JVM이 `DeviceHubApplication.kt`의 `main` 함수를 실행합니다.
2. `runApplication<DeviceHubApplication>()`이 Spring Boot를 시작합니다.
3. Spring은 설정을 자동 구성하고 `com.devicehub` 아래의 컴포넌트를 탐색합니다.
4. `HealthController`를 찾아 Spring Bean으로 등록하고 `/api/health` 요청 경로와 연결합니다.
5. 내장 웹 서버가 기본 포트 8080에서 HTTP 요청을 기다립니다.

### 주요 어노테이션

- `@SpringBootApplication`: 설정 클래스임을 나타내고, Spring Boot 자동 설정과 컴포넌트 스캔을 활성화합니다. 이 클래스가 `com.devicehub`에 있으므로 하위 패키지들이 탐색 대상이 됩니다.
- `@RestController`: 클래스를 HTTP 요청을 처리하는 컨트롤러로 등록합니다. 함수의 반환값은 View 이름이 아니라 HTTP 응답 본문으로 변환됩니다.
- `@RequestMapping("/api")`: 컨트롤러에 속한 모든 요청 경로의 공통 접두사를 지정합니다.
- `@GetMapping("/health")`: HTTP GET 요청과 `health()` 함수를 연결합니다. 클래스 경로와 합쳐져 `/api/health`가 됩니다.

### Health 요청 흐름

1. 클라이언트가 `GET /api/health`를 보냅니다.
2. 내장 웹 서버가 요청을 받아 Spring MVC의 `DispatcherServlet`에 전달합니다.
3. Spring MVC가 등록된 요청 매핑에서 `HealthController.health()`를 찾습니다.
4. `health()`가 `HealthResponse(status = "UP")` 객체를 반환합니다.
5. Spring Web에 포함된 Jackson이 Kotlin 객체를 JSON `{"status":"UP"}`로 변환합니다.
6. Spring MVC가 HTTP 200과 JSON 응답을 클라이언트에 보냅니다. 별도 상태 코드를 지정하지 않았으므로 정상 반환의 기본값인 200이 사용됩니다.

## 관리자 웹 UI/UX 개선

현재 단계에서는 기존 Device CRUD 기능과 API 호출 로직을 유지하고 관리자 화면의 표현만 정돈했습니다.

- 1024px 이상 화면에서 사이드바, 헤더, 본문 여백이 일정하게 이어지도록 레이아웃 폭과 간격을 조정했습니다.
- 버튼과 입력 컨트롤 높이를 40px 기준으로 통일했습니다.
- 요약 영역의 불필요한 박스 표현과 드로어·확인창의 그림자를 제거하고 색상 사용을 줄였습니다.
- 기기 테이블의 열 너비, 행 높이, 헤더 대비를 정돈하고 긴 이름·제조사·모델명·OS 버전은 말줄임표와 전체 텍스트 툴팁으로 처리했습니다.
- 상세 정보의 긴 값은 줄바꿈되어 드로어 밖으로 넘치지 않도록 했습니다.
- 로딩, 빈 목록, 검색 결과 없음, API 오류 상태를 목록 영역 안에서 일관된 형태로 표시합니다.

### 프론트엔드 학습 내용

- CSS Module은 컴포넌트별 클래스 이름 충돌을 막으면서 화면 스타일을 분리합니다.
- CSS 변수로 색상, 간격, 컨트롤 높이를 관리하면 여러 화면의 UI 규격을 한 곳에서 맞출 수 있습니다.
- `table-layout: fixed`와 열 너비 지정은 데이터 길이가 달라도 테이블 구조가 흔들리지 않게 합니다.
- `text-overflow: ellipsis`는 한 줄 표 셀을 안정적으로 유지하고, `overflow-wrap: anywhere`는 상세 화면의 긴 문자열을 안전하게 줄바꿈합니다.
- React의 로딩·오류·빈 배열 상태를 각각 분기하면 사용자가 현재 목록 상태와 다음 행동을 명확히 알 수 있습니다.

## ADB 연결 기기 자동 감지 및 등록

관리자 웹의 **기기 등록** 버튼을 누르면 Spring Boot 서버가 실행되는 PC에서 ADB 연결 기기를 검색합니다. Android 기기를 찾으면 제조사, 모델, Android 버전, SDK 버전, Serial과 기기 타입을 보여주고 기존 등록 폼에 값을 채웁니다. 이름과 타입을 확인하거나 수정한 뒤 기존 POST /api/devices로 등록합니다.

### 준비 사항

1. PC에 Android SDK Platform Tools의 adb가 설치되어 있어야 합니다.
2. Android 기기에서 개발자 옵션과 USB 디버깅을 활성화합니다.
3. 데이터 통신이 가능한 USB 케이블로 Spring Boot 서버 PC에 기기를 연결합니다.
4. 기기에 RSA 승인 창이 나타나면 연결을 허용합니다. 개인 개발 PC라면 “이 컴퓨터에서 항상 허용”을 선택할 수 있습니다.
5. adb version과 adb devices 명령으로 연결 상태가 device인지 확인합니다.

ADB가 PATH에 없다면 실행 전에 ADB_PATH 환경변수에 실행 파일 전체 경로를 지정할 수 있습니다. 별도 설정이 없으면 서버는 ANDROID_HOME, ANDROID_SDK_ROOT, Windows의 기본 Android SDK 경로, PATH 순서로 adb를 찾습니다. ADB 명령에는 5초 timeout을 적용하며, 명령과 인자를 분리하는 ProcessBuilder를 사용합니다.

### 연결 상태

- CONNECTED: 정상 기기 한 대를 감지했습니다.
- NOT_FOUND: 연결된 기기가 없습니다. 관리자 웹에서 다시 검색하거나 기존 수동 등록 폼을 사용할 수 있습니다.
- UNAUTHORIZED: 기기 화면에서 USB 디버깅 연결을 승인해야 합니다. 승인 후 다시 검색합니다.
- OFFLINE: USB 케이블을 다시 연결하고 USB 디버깅 상태를 확인합니다.
- MULTIPLE: 정상 연결된 기기가 여러 대이므로 관리자 웹에서 한 대를 선택합니다. 서버가 임의로 선택하지 않습니다.
- ALREADY_REGISTERED: 같은 Serial이 이미 등록되어 있어 신규 등록을 차단합니다.
- ADB_NOT_AVAILABLE: adb 실행 파일을 찾을 수 없습니다.
- ERROR: adb 명령 또는 기기 정보 조회에 실패했습니다.

### API와 타입 판단

GET /api/devices/connected는 서버 PC의 adb devices 결과를 읽고, device 상태인 기기에만 adb -s {serial} shell getprop 명령을 실행합니다. serial은 HTTP 요청으로 받지 않고 바로 앞의 adb devices 출력에서 얻은 값만 사용합니다.

serialNumber, manufacturer, modelName, productName, deviceName, osVersion, sdkVersion, type을 반환합니다. 기기 타입은 wm size와 wm density로 계산한 smallest width가 Android의 일반적인 large-screen 기준인 600dp 이상이면 TABLET, 미만이면 PHONE으로 판단합니다. 화면 정보를 읽지 못하면 임의 지정하지 않고 등록 폼에서 사용자가 선택하게 합니다.

### Serial 저장과 중복 방지

기존 수동 등록 데이터에 영향을 주지 않도록 serial_number는 nullable 컬럼입니다. 값이 존재하는 행에만 적용되는 PostgreSQL partial unique index로 같은 ADB 기기의 중복 등록을 DB에서도 방지합니다. 서비스는 저장 전 findBySerialNumber로 먼저 확인하며 중복 요청에는 HTTP 409를 반환합니다. 기존 수동 등록은 Serial 없이 계속 사용할 수 있습니다.

### 현재 제한사항

웹 브라우저가 adb를 직접 실행하는 구조가 아니라 Spring Boot 서버 프로세스가 adb를 실행합니다. 따라서 현재 방식은 **Spring Boot 서버가 실행되는 PC에 USB로 직접 연결된 기기만** 감지할 수 있습니다. 서버를 원격 환경에 배포하면 관리자 PC의 USB 기기를 볼 수 없으므로, 추후에는 관리자 PC에서 동작하는 별도의 DeviceHub Agent가 필요할 수 있습니다.
