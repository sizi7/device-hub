# DeviceHub

## React 관리자 웹 진행 상태

React, Vite, JavaScript, axios, CSS Modules로 DeviceHub 관리자 웹을 구성했다. 별도 UI 라이브러리와 전역 상태 관리 도구는 사용하지 않고 `useState`, `useEffect`를 사용한다.

현재 실제 기능이 연결된 메뉴는 `Devices`와 `Projects`이며 Dashboard, Apps, Users, Settings는 향후 기능을 위한 화면 구조만 제공한다.

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
- Phase 3 완료: Device Entity와 Device CRUD API
- Phase 3.5 완료: Swagger/OpenAPI(springdoc) 문서화
- ADB 연결 기기 자동 감지 및 등록 완료
- 병원 배치(DeviceDeployment) 관리 완료
- DeviceProject 기반 기기별 앱 설치 버전 관리 완료
- 독립 Project 관리 완료: Project CRUD, DeviceProjectAssignment 할당 이력, 프로젝트 네트워크, 프로젝트 APK 업로드·다운로드
- 프로젝트 키스토어 완료: 키스토어 업로드·검증·다운로드·삭제와 AES-GCM 비밀번호 암호화 저장, 복호화 조회
- React 관리자 웹 완료: Devices, Projects 메뉴에 실제 API 연동. Dashboard, Apps, Users, Settings는 화면 구조만 제공
- 인증·인가 완료: Spring Security와 JWT 로그인, UserRole 3종, 키스토어 API 역할 제한, 관리자 웹 로그인 화면

다음 단계(미진행):

- 키스토어 암호화 마스터 키 교체. DEFAULT_SECRET_KEY를 없애려면 기존 암호문 재암호화 절차가 먼저 필요합니다.
- 키스토어와 Project API의 통합 테스트. 현재 테스트는 `SecretEncryptorTest`, `JwtTokenProviderTest`, `KeystoreSecurityTest`, `HealthControllerTest`입니다.

현재는 Spring Web, Spring Data JPA, Spring Validation, Spring Security, springdoc-openapi, jjwt, Jackson Kotlin Module, PostgreSQL JDBC Driver를 사용합니다. Docker는 아직 추가하지 않았습니다.

## 기술 환경

백엔드

- Java 21
- Kotlin 1.9.25
- Spring Boot 3.4.5
- Gradle Kotlin DSL
- Spring Web
- Spring Data JPA
- Spring Validation
- Spring Security
- jjwt 0.12.6 (JWT)
- Jackson Kotlin Module
- springdoc-openapi 2.8.9 (Swagger UI)
- PostgreSQL 17.11

프론트엔드

- React 19.1
- Vite 7.1
- JavaScript, CSS Modules
- axios 1.11

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

## 병원 배치 관리

기기가 병원으로 나가는 경우를 DeviceDeployment 이력으로 관리합니다.

- HOSPITAL_LOAN: 일정 기간 병원에 임시로 대여하는 배치입니다.
- HOSPITAL_DEDICATED: 특정 병원에서 계속 사용하는 전용 기기 배치입니다.

Device에 boolean이나 현재 병원명만 저장하면 회수 시 과거 정보를 잃기 때문에 배치 건마다 별도 행을 생성합니다. returnedAt이 null이면 현재 배치 중이고, 회수하면 해당 행의 returnedAt만 기록하므로 과거 병원·유형·기간·메모가 유지됩니다. 한 기기에 returnedAt이 null인 행이 하나만 존재하도록 서비스 검사와 PostgreSQL partial unique index를 함께 사용합니다.

제공 API:

- POST /api/devices/{deviceId}/deployments: 병원 배치
- POST /api/devices/{deviceId}/deployments/return: 현재 배치 회수
- GET /api/devices/{deviceId}/deployments: 전체 배치 이력
- GET /api/devices/{deviceId}/deployments/current: 현재 배치 상태

현재 hospitalName은 단순 문자열이므로 Hospital Entity에 강하게 결합되지 않습니다. 병원 자체 정보 관리가 필요해지는 단계에서 별도 Entity와 관계로 확장할 수 있습니다.

## DeviceProject와 버전 관리

한 Device에 여러 DeviceProject를 등록해 프로젝트명, Android packageName, 설치 버전, 최신 배포 버전과 마지막 업데이트 시각을 관리합니다.

- installedVersion: 현재 기기에 실제로 설치된 버전입니다.
- latestVersion: 현재 배포 기준으로 설치되어야 하는 최신 버전입니다.
- lastUpdatedAt: 해당 기기의 앱을 마지막으로 설치하거나 업데이트한 시각입니다.
- packageName: 현재는 선택값이며, 추후 adb shell dumpsys package 명령으로 설치 버전을 자동 조회할 때 사용할 수 있습니다.

versionStatus는 버전 크기를 문자열로 비교하지 않습니다. 두 버전이 모두 있고 동일하면 LATEST, 두 값이 다르면 UPDATE_REQUIRED, 하나라도 없으면 UNKNOWN입니다. 이 단계에서는 정확한 semantic version 규칙을 정의하지 않았으므로 1.10과 1.9를 임의로 서열화하지 않고 동일 여부만 판단합니다.

제공 API:

- POST /api/devices/{deviceId}/projects
- GET /api/devices/{deviceId}/projects
- GET /api/devices/{deviceId}/projects/{projectId}
- PUT /api/devices/{deviceId}/projects/{projectId}
- DELETE /api/devices/{deviceId}/projects/{projectId}

관리자 웹의 Device 상세 화면에서 현재 병원 상태, 배치·회수, 배치 이력과 프로젝트 추가·수정·삭제 및 버전 상태를 확인할 수 있습니다. Device 목록에는 상세 데이터를 모두 펼치지 않고 프로젝트 수와 현재 위치만 표시합니다.

### 학습 내용

- 현재 상태와 이력을 함께 관리해야 하는 데이터는 원본 Device의 boolean보다 별도 이력 Entity가 적합합니다.
- returnedAt이 없는 행을 현재 상태로 해석하면 회수된 기록을 삭제하지 않고도 현재와 과거를 구분할 수 있습니다.
- DB unique index는 동시에 들어오는 중복 배치 요청도 최종적으로 방어합니다.
- DeviceProject는 Device와 다대일 관계이므로 한 기기에 여러 앱 정보를 독립적으로 저장할 수 있습니다.
- 패키지명을 미리 저장하면 다음 단계에서 ADB 기반 설치 버전 조회로 확장하기 쉽습니다.

## 독립 Project 관리 시스템

Project는 Device와 별도의 마스터 Entity로 프로젝트명, 식별 코드, 설명, 관리자와 진행 상태를 관리합니다. 상태는 PLANNING, DEVELOPMENT, OPERATING, SUSPENDED, COMPLETED로 구분하며 code는 중복될 수 없습니다.

Device 자체에 현재 프로젝트 문자열만 저장하면 과거 참여 이력을 잃기 때문에 DeviceProjectAssignment를 사용합니다. assignedAt은 할당 시작, endedAt은 종료 시각이며 endedAt이 null인 행이 현재 할당입니다. 한 Device에는 활성 할당이 하나만 존재하도록 서비스 검사와 PostgreSQL partial unique index를 함께 적용합니다. 프로젝트 상세에서는 현재 할당된 Device 목록을 역방향으로 조회할 수 있습니다.

기존 DeviceProject는 삭제하지 않았습니다. 이 구조는 기기별 앱과 installedVersion, latestVersion을 관리하는 기존 역할을 유지합니다. 현재 DB와 Entity에는 Device.project 문자열 필드가 존재하지 않아 자동 migration 대상이 없습니다. Project와 이름이 같은 기존 DeviceProject가 있으면 설치 버전 비교에 활용하지만, 이름이 일치하지 않는 데이터를 임의로 연결하지 않습니다.

### 프로젝트 네트워크

ProjectNetwork는 한 프로젝트의 환경별 접속 주소를 관리합니다.

- ISO: ISO 검증 환경
- MFDS: 인허가·규제 대응 환경
- DEVELOPMENT: 개발 및 테스트 환경
- BUSINESS: 사업부 운영 환경

현재는 환경 이름, API URL, Socket URL과 설명만 저장합니다. Password, API Key, Secret, Token은 평문 DB 컬럼으로 추가하지 않습니다. 민감정보 중 키스토어 비밀번호만 예외적으로 아래 프로젝트 키스토어에서 암호화 저장 방식으로 다룹니다.

### 프로젝트 APK

ProjectApk는 version, Android versionCode, 환경, 원본 파일명, 저장 경로, 릴리즈 노트와 업로드 시각을 관리합니다. APK binary는 PostgreSQL에 저장하지 않고 기본적으로 storage/apks/{projectCode}/{version} 아래에 UUID 파일명으로 저장합니다. 저장 위치는 APK_STORAGE_PATH 환경변수로 변경할 수 있습니다.

- .apk 확장자만 허용합니다.
- 파일당 최대 크기는 200MB입니다.
- UUID 저장 파일명으로 충돌을 방지합니다.
- 다운로드 시에는 원본 파일명을 제공합니다.
- 환경별 가장 최근 uploadedAt을 최신 APK로 조회합니다.
- APK 삭제 시 DB 메타데이터와 로컬 파일을 함께 삭제합니다.

DeviceProjectAssignment 응답은 프로젝트명과 일치하는 기존 DeviceProject의 installedVersion과 프로젝트의 최신 업로드 APK 버전을 비교합니다. 둘이 같으면 LATEST, 다르면 UPDATE_REQUIRED, 한쪽이 없으면 UNKNOWN입니다. semantic version 크기 비교는 하지 않습니다.

### 프로젝트 키스토어

ProjectKeystore는 프로젝트별 APK 서명 키스토어와 비밀번호를 관리합니다. 키스토어 파일은 DB에 넣지 않고 기본적으로 storage/keystores/{projectCode} 아래에 UUID 파일명으로 저장하며, 저장 위치는 KEYSTORE_STORAGE_PATH 환경변수로 변경할 수 있습니다.

- .jks, .keystore, .p12, .pfx 확장자만 허용합니다.
- 파일당 최대 크기는 10MB입니다.
- 파일 앞 4byte로 형식을 판별합니다. JKS는 magic number 0xFEEDFEED, PKCS12는 DER SEQUENCE 0x30으로 시작합니다.
- 업로드 시 실제로 KeyStore를 열어 스토어 비밀번호, alias 존재 여부, 키 비밀번호를 모두 검증한 뒤에만 저장합니다. 검증에 실패하면 저장한 파일을 지우고 400을 반환합니다.
- 키 비밀번호를 비우면 스토어 비밀번호와 같다고 보고 keyPasswordEnc를 null로 저장합니다.
- 키스토어 삭제 시 DB 메타데이터와 로컬 파일을 함께 삭제합니다.

#### 비밀번호 암호화

로그인 비밀번호와 달리 키스토어 비밀번호는 실제 APK 서명에 원문이 필요하므로 단방향 해시를 쓸 수 없습니다. SecretEncryptor가 AES-GCM 대칭키 암호화로 처리합니다.

- 마스터 키는 devicehub.security.secret-key로 주입하며 환경변수는 KEYSTORE_SECRET_KEY입니다.
- 주입한 문자열을 SHA-256으로 32byte AES 키로 변환합니다.
- 암호화마다 12byte IV를 새로 생성하고 "IV + 암호문"을 Base64로 저장합니다. 같은 비밀번호라도 매번 암호문이 달라 서로 비교당하지 않습니다.
- KEYSTORE_SECRET_KEY를 설정하지 않으면 개발용 기본 키를 쓰고 시작 시 WARN 로그를 남깁니다. 운영 환경에서는 반드시 설정해야 합니다.
- 마스터 키를 바꾸면 기존에 저장한 비밀번호는 복호화할 수 없습니다.

목록과 상세 응답에는 비밀번호를 절대 포함하지 않습니다. 복호화한 값은 POST /reveal 로만 조회하며, URL과 브라우저 기록에 남지 않도록 GET이 아닌 POST로 제공합니다.

아직 인증과 권한 기능이 없으므로 API에 접근할 수 있는 사람은 누구나 비밀번호를 조회할 수 있습니다. 외부에 노출되는 환경에 배포하기 전에 인증, 권한과 감사 로그가 필요합니다.

키스토어 검증 실패 사유를 화면에 그대로 보여주기 위해 server.error.include-message를 always로 설정했습니다. 프론트에서는 projectApi.js의 getServerErrorMessage가 응답 body의 message를 우선 사용하고, message가 없으면 기존 getApiErrorMessage로 넘어갑니다.

#### 보안 점검 결과 (2026-09-02)

키스토어 관리 기능을 코드, DB, 파일 저장, API, 로그, 프론트엔드 기준으로 점검했습니다. 실제 비밀번호와 마스터 키 값은 점검 과정과 기록 어디에도 남기지 않았습니다.

문제가 없다고 확인한 항목입니다.

- DB에 평문 비밀번호가 없습니다. project_keystore에는 store_password_enc와 key_password_enc만 존재하며 저장값은 AES-GCM 암호문입니다.
- 목록과 상세 응답에 비밀번호와 서버 내부 filePath가 포함되지 않습니다.
- 업로드 파일은 확장자, 파일 앞 4byte magic number, 실제 KeyStore.load와 alias·키 비밀번호 검증까지 3단계로 확인합니다. 확장자만 바꾼 파일은 400으로 거부하고 저장한 파일을 지웁니다.
- 저장 파일명은 UUID, 디렉터리는 projectCode를 sanitize한 값이며 storageRoot 밖으로 나가지 못합니다. 파일명에 ../ 를 넣어도 storage 밖에 파일이 생기지 않습니다.
- 키스토어 파일은 정적 리소스 경로와 분리된 storage/keystores 아래에 있고 .gitignore로 제외됩니다.
- 비밀번호를 로그로 남기는 코드가 없습니다. 백엔드 로거 사용은 SecretEncryptor의 기본 키 경고 한 곳뿐입니다.
- 프론트엔드는 비밀번호를 localStorage나 sessionStorage에 저장하지 않고 console에도 출력하지 않습니다. 입력은 type="password"로 가립니다.
- APK 서명 기능이 없으므로 비밀번호를 커맨드라인 인자로 넘기거나 임시 파일에 쓰는 경로가 없습니다. ProcessBuilder는 ADB 호출 한 곳뿐이며 shell을 거치지 않습니다.
- CORS를 따로 열어두지 않았습니다. allowedOrigins를 "*"로 설정한 곳이 없습니다.
- Swagger 문서에 비밀번호 example 값이 없습니다.

이번에 조치한 항목입니다.

- POST /reveal 과 GET /download 응답에 Cache-Control: no-store를 추가했습니다. 복호화한 비밀번호와 키스토어 파일이 브라우저나 중간 캐시에 남지 않도록 합니다.
- .gitignore에 *.jks, *.keystore, *.p12, *.pfx 패턴을 추가했습니다. storage/ 밖에 키스토어를 두더라도 커밋되지 않습니다.

아직 남은 위험과 별도 작업 항목입니다.

- (해결) 인증과 권한이 없던 문제는 Spring Security와 JWT 도입으로 해결했습니다. 아래 "인증과 인가" 항목을 참고해 주세요.
- KEYSTORE_SECRET_KEY를 설정하지 않으면 소스와 application.yml에 있는 기본 키를 사용합니다. 이 값은 저장소에 공개되어 있으므로 DB 덤프만 유출되어도 저장된 비밀번호를 복호화할 수 있습니다. 기본값을 제거하려면 기존 암호문을 새 키로 다시 암호화하는 절차가 먼저 필요합니다.
- (해결) Swagger UI는 운영 프로파일에서 비활성화하고, 개발 환경에서도 Authorize로 token을 넣어야 보호된 API를 호출할 수 있습니다.
- server.error.include-message가 always라서 예외 메시지가 그대로 전달됩니다. 지금 키스토어 관련 메시지에는 민감정보가 없지만, 앞으로 RestControllerAdvice로 노출 메시지를 통제한 뒤 회수하는 편이 안전합니다.
- delete는 파일을 먼저 지우고 DB 삭제는 커밋 시점에 반영됩니다. 커밋이 실패하면 파일만 사라집니다. upload도 커밋 단계에서 실패하면 storage에 파일이 남습니다.
- (해결) 키스토어 조회·다운로드·삭제 등에 감사 로그를 남깁니다.

인증과 인가를 도입한 뒤에도 마스터 키 문제가 남아 있으므로 운영 서버 배포 전에는 마스터 키 정비가 필요합니다. 실수로 다른 기기에서 접근하는 일이 없도록 server.address 기본값은 127.0.0.1로 둡니다. 다른 기기에서 접속해야 하면 SERVER_ADDRESS=0.0.0.0 으로 실행합니다.

### Project API

- POST /api/projects
- GET /api/projects
- GET /api/projects/{id}
- PUT /api/projects/{id}
- DELETE /api/projects/{id}
- POST /api/devices/{deviceId}/project-assignments
- GET /api/devices/{deviceId}/project-assignments
- GET /api/devices/{deviceId}/project-assignments/current
- POST /api/devices/{deviceId}/project-assignments/end
- GET /api/projects/{projectId}/devices
- /api/projects/{projectId}/networks 하위 CRUD
- /api/projects/{projectId}/apks 하위 업로드, 조회, 다운로드, 최신 조회와 삭제
- POST /api/projects/{projectId}/keystores (multipart 업로드)
- GET /api/projects/{projectId}/keystores
- GET /api/projects/{projectId}/keystores/{keystoreId}
- POST /api/projects/{projectId}/keystores/{keystoreId}/reveal (비밀번호 복호화 조회)
- PUT /api/projects/{projectId}/keystores/{keystoreId} (이름·설명 수정)
- PUT /api/projects/{projectId}/keystores/{keystoreId}/password (alias·비밀번호 변경)
- GET /api/projects/{projectId}/keystores/{keystoreId}/download
- DELETE /api/projects/{projectId}/keystores/{keystoreId}

관리자 웹의 Projects 메뉴에서는 프로젝트 목록과 개요, 연결 기기, 네트워크, APK, 키스토어 탭을 제공합니다. 키스토어 탭에서는 파일과 alias, 비밀번호를 함께 등록하고 목록에서 비밀번호 보기, 비밀번호 변경, 다운로드, 삭제를 할 수 있습니다. 비밀번호 입력은 type="password"로 가리고, 목록에는 비밀번호를 표시하지 않으며 비밀번호 보기를 눌렀을 때만 별도 Dialog로 보여줍니다. 서명 키스토어를 잃으면 같은 서명으로 앱을 업데이트할 수 없으므로 삭제 전에 확인 창을 띄웁니다. Device 상세의 현재 프로젝트 영역에서는 프로젝트 할당·종료, 설치 버전, 최신 APK 버전과 과거 이력을 확인할 수 있습니다.

### Project 학습 내용

- Project와 Device를 독립 Entity로 두면 프로젝트 정보 중복 없이 여러 기기와 연결할 수 있습니다.
- 현재 관계와 과거 이력이 모두 필요하면 중간 이력 Entity에 시작·종료 시각을 저장합니다.
- multipart/form-data는 파일과 버전 메타데이터를 한 요청으로 전달합니다.
- 파일 binary와 검색 가능한 메타데이터를 분리하면 DB 크기와 다운로드 처리를 단순하게 유지할 수 있습니다.
- Swagger UI에서 Projects, Project Networks, Project APKs, Project Keystores, Device Project Assignments 태그별 요청 필드, 상태 코드와 multipart 파라미터 설명을 확인할 수 있습니다.
- 되돌릴 필요가 있는 비밀값은 해시가 아니라 대칭키 암호화로 저장하고, 마스터 키는 코드나 DB가 아니라 환경변수로 분리합니다.
- AES-GCM은 암호화와 무결성 검증을 함께 제공하므로 저장한 암호문이 변조되면 복호화 단계에서 예외가 발생합니다.
- @Value로 설정값을 주입하는 @Component를 만들면 암호화 같은 공통 기능을 서비스에서 재사용할 수 있습니다.
- JDK 9부터 JKS와 PKCS12 KeyStore 구현이 서로의 형식도 읽어주기 때문에, KeyStore.load 성공 여부로는 실제 파일 형식을 구분할 수 없고 파일 magic number를 봐야 합니다.
- 파일을 먼저 저장한 뒤 검증에 실패하면 try-catch에서 저장한 파일을 지워야 storage에 쓰레기 파일이 남지 않습니다.

## 인증과 인가 (Spring Security + JWT)

보안 점검에서 확인한 접근 통제 문제를 해결하기 위해 Spring Security와 JWT 기반 로그인을 도입했습니다. 기존 API path와 함수 이름은 그대로 두고 권한 검사만 덧붙였습니다.

### 구조

- `spring-boot-starter-security`와 `io.jsonwebtoken:jjwt` 0.12.6을 사용합니다.
- `SecurityConfig`가 `SecurityFilterChain`을 구성합니다. 세션은 `STATELESS`, form login과 http basic은 끕니다.
- 권한 정책을 두 층으로 나눕니다. `SecurityConfig`의 URL 규칙은 "인증이 필요한가"만 정하고, 역할별 제한은 컨트롤러 메서드의 `@PreAuthorize`가 담당합니다. 키스토어는 같은 경로에 HTTP 메서드만 다른 API가 여러 개라서 URL 패턴으로 역할을 나누면 규칙이 어긋나기 쉽고, 권한 규칙을 엔드포인트 옆에 두면 한 곳만 보면 되기 때문입니다.
- `permitAll`을 선언한 곳 외에는 모두 인증이 필요한 deny-by-default 방식입니다.

인증 없이 접근할 수 있는 경로는 다음뿐입니다.

- `GET /api/health`
- `POST /api/auth/login`
- `/error` (컨트롤러 예외가 ERROR 디스패치로 한 번 더 지나가는 경로입니다. 막으면 로그인 실패 401이 엉뚱한 메시지로 덮입니다.)
- 개발 환경의 Swagger 경로 (`/swagger-ui/**`, `/v3/api-docs/**`)

### UserRole

`app_user` 테이블에 사용자를 저장하며 역할은 세 가지입니다.

- `ROLE_ADMIN`: 전체 관리. 키스토어 모든 작업과 사용자 관리
- `ROLE_RELEASE_MANAGER`: 배포 관련 관리. 키스토어 등록·조회·다운로드·삭제
- `ROLE_USER`: 일반 조회와 Device·Project 관리. 키스토어 secret과 파일에는 접근 불가

Spring Security의 `hasRole`은 `ROLE_` 접두사를 자동으로 붙여서 접두사가 두 번 붙는 실수가 생기기 쉽습니다. 그래서 enum 이름 자체에 `ROLE_` 접두사를 포함해 DB에 그대로 저장하고, 권한 검사는 접두사를 붙이지 않는 `hasAnyAuthority`로만 통일했습니다.

### 로그인 API

```
POST /api/auth/login
{ "username": "...", "password": "..." }
```

응답에는 `accessToken`, `tokenType`, `expiresIn`, `user`(id, username, name, role)가 담깁니다. 이후 요청에는 다음 헤더를 붙입니다.

```
Authorization: Bearer {accessToken}
```

`GET /api/auth/me`로 현재 token에 연결된 사용자를 확인할 수 있습니다.

- 잘못된 username 또는 password: `401`. 어느 쪽이 틀렸는지 구분해서 알려주지 않습니다. 구분하면 어떤 계정이 존재하는지 알려주는 셈이 됩니다.
- 비활성화된 계정: `403`

### 사용자 비밀번호

로그인 비밀번호는 `BCryptPasswordEncoder`로 해시해서 저장하며 평문은 어디에도 남기지 않습니다. 응답과 로그에도 포함하지 않습니다. 키스토어 비밀번호는 APK 서명에 원문이 필요해서 `SecretEncryptor`로 복호화 가능하게 암호화하지만, 로그인 비밀번호는 원문이 필요하지 않으므로 단방향 해시를 씁니다. 두 용도를 섞어 쓰지 않습니다.

### JWT_SECRET

JWT 서명 키는 `JWT_SECRET` 환경변수로 주입합니다. 키스토어 마스터 키에서 겪은 문제를 반복하지 않기 위해 **소스에 기본값을 두지 않습니다.**

- `JWT_SECRET`이 있으면 그 값을 씁니다. 32byte 미만이면 기동을 실패시킵니다.
- `prod` 프로파일에서 `JWT_SECRET`이 없으면 기동을 실패시킵니다.
- 그 외(로컬 개발)에서 없으면 기동할 때마다 무작위 키를 새로 만들고 WARN 로그를 남깁니다. 하드코딩 값이 저장소에 남지 않는 대신 서버를 재시작하면 기존 token이 모두 무효가 됩니다.

유효 시간은 `JWT_EXPIRATION_SECONDS`로 조정하며 기본값은 3600초입니다.

### 초기 관리자 생성

`admin/admin123` 같은 고정 계정을 코드에 넣지 않습니다. 아래 두 환경변수가 모두 설정되고 아직 `ROLE_ADMIN` 사용자가 없을 때만 최초 관리자를 만듭니다. 이미 관리자가 있으면 아무것도 하지 않고, 비밀번호는 로그로 남기지 않습니다.

```powershell
$env:DEVICEHUB_ADMIN_USERNAME = "원하는_아이디"
$env:DEVICEHUB_ADMIN_PASSWORD = "8자 이상 비밀번호"
$env:JWT_SECRET = "32byte 이상 임의 문자열"
.\gradlew.bat bootRun
```

이후 사용자는 `ROLE_ADMIN`이 `POST /api/users`로 추가합니다. `GET /api/users`도 `ROLE_ADMIN` 전용이며 두 응답 모두 비밀번호를 포함하지 않습니다. 사용자 관리 화면은 이번 단계에서 만들지 않았고 API까지만 제공합니다.

### 키스토어 역할 제한

`ProjectKeystoreController`의 각 메서드에 `@PreAuthorize`를 붙였습니다. 기존 API path는 변경하지 않았습니다.

| API | 필요 권한 |
| --- | --- |
| `GET /api/projects/{projectId}/keystores` | 인증된 사용자 |
| `GET /api/projects/{projectId}/keystores/{keystoreId}` | 인증된 사용자 |
| `POST /api/projects/{projectId}/keystores` | ADMIN, RELEASE_MANAGER |
| `POST /api/projects/{projectId}/keystores/{keystoreId}/reveal` | ADMIN, RELEASE_MANAGER |
| `PUT /api/projects/{projectId}/keystores/{keystoreId}` | ADMIN, RELEASE_MANAGER |
| `PUT /api/projects/{projectId}/keystores/{keystoreId}/password` | ADMIN, RELEASE_MANAGER |
| `GET /api/projects/{projectId}/keystores/{keystoreId}/download` | ADMIN, RELEASE_MANAGER |
| `DELETE /api/projects/{projectId}/keystores/{keystoreId}` | ADMIN, RELEASE_MANAGER |

목록과 상세는 비밀번호와 서버 내부 경로를 포함하지 않으므로 인증만 요구합니다. `ROLE_USER`가 secret이나 실제 키 파일에 닿는 API를 호출하면 403입니다.

### 401과 403

`SecurityErrorResponder`가 HTML 오류 페이지 대신 JSON을 반환합니다.

```json
{ "status": 401, "error": "UNAUTHORIZED", "message": "Authentication is required." }
{ "status": 403, "error": "FORBIDDEN", "message": "You do not have permission to access this resource." }
```

- `401`: 인증이 없거나 token이 유효하지 않음
- `403`: 로그인은 되어 있지만 권한이 없음

응답에 stack trace나 내부 경로를 담지 않습니다.

### 감사 로그

`SecurityAuditLogger`가 `com.devicehub.audit` 로거로 키스토어 민감 작업을 기록합니다. 남기는 값은 action, userId, username, role, projectId, keystoreId, success입니다. 비밀번호, JWT, 마스터 키, 키스토어 파일 내용은 인자로 받지 않습니다.

기록 대상은 `KEYSTORE_UPLOAD`, `KEYSTORE_REVEAL`, `KEYSTORE_DOWNLOAD`, `KEYSTORE_PASSWORD_UPDATE`, `KEYSTORE_DELETE`이며 실패한 시도도 남깁니다. 별도 Audit Entity는 만들지 않고 애플리케이션 로그로 시작합니다.

### CSRF와 CORS

- CSRF는 끕니다. Bearer token을 `Authorization` 헤더로 전달하는 stateless API여서 브라우저가 자동으로 실어 보내는 쿠키가 없고, CSRF 공격 경로 자체가 성립하지 않습니다. 나중에 HttpOnly 쿠키 인증으로 바꾼다면 CSRF 보호를 다시 켜야 합니다.
- CORS는 `http://localhost:5173`과 `http://127.0.0.1:5173`만 허용합니다. `allowedOrigins="*"`는 쓰지 않고 `allowCredentials`는 false입니다. 관리자 웹은 Vite proxy를 거치므로 실제로는 같은 origin이지만, 개발 중 직접 호출할 때를 위해 최소 범위로 열어 둡니다.

### Swagger 인증

Swagger UI 오른쪽 위 `Authorize` 버튼에 로그인 응답의 `accessToken` 값을 입력하면 이후 `Try it out` 요청에 `Authorization: Bearer {token}` 헤더가 자동으로 붙습니다. `Bearer` 접두사는 직접 입력하지 않습니다.

`GET /api/health`와 `POST /api/auth/login`은 `@SecurityRequirements`로 공개 API임을 표시했습니다.

개발 환경과 운영 환경의 Swagger 노출이 다릅니다.

- 개발: Swagger UI와 api-docs 사용 가능
- 운영(`SPRING_PROFILES_ACTIVE=prod`): `application-prod.yml`이 `springdoc.api-docs.enabled`와 `springdoc.swagger-ui.enabled`를 false로 두어 두 경로 모두 404

### 관리자 웹 로그인

로그인 화면(`features/auth/LoginPage.jsx`)을 추가했습니다. 로그인하지 않으면 다른 화면으로 들어갈 수 없습니다. 헤더에는 사용자 이름과 역할이 표시되고 로그아웃 버튼이 있습니다.

token은 `sessionStorage`에 보관합니다. 메모리에만 두면 새로고침할 때마다 로그아웃되고, `localStorage`에 두면 탭을 닫아도 남습니다. `sessionStorage`는 탭을 닫으면 사라지므로 이 규모에서 다루기 가장 단순한 절충입니다. 다만 XSS가 발생하면 스크립트가 읽을 수 있다는 한계가 있어, 더 강한 보호가 필요하면 HttpOnly 쿠키와 CSRF 보호로 옮겨야 합니다.

`authApi.js`의 `attachAuthInterceptors`가 기존 `deviceApi`와 `projectApi`의 axios 인스턴스에 공통으로 붙습니다. API 호출마다 token 코드를 반복하지 않습니다.

- `401`: 인증 상태를 지우고 로그인 화면으로 돌아갑니다.
- `403`: 로그인 화면으로 보내지 않고 권한 부족 메시지만 보여줍니다.

키스토어 탭에서는 `ROLE_USER`에게 등록 폼과 비밀번호 보기·변경·다운로드·삭제 버튼을 숨깁니다. 화면에서 숨기는 것은 편의를 위한 보조 수단이고 실제 차단은 백엔드 `@PreAuthorize`가 담당합니다. 비밀번호 보기 Dialog는 닫으면 즉시 state에서 지우고, 방치되면 45초 후 자동으로 닫힙니다.

### 이번 단계에서 하지 않은 것

키 교체는 기존 암호문을 복호화할 수 없게 만들 수 있어 migration 절차와 함께 별도 단계로 분리했습니다.

- 키스토어 `DEFAULT_SECRET_KEY` 제거
- 기존 키스토어 비밀번호 재암호화와 마스터 키 rotation
- APK 실제 서명 기능
- Refresh Token, OAuth, SSO, 조직·부서 기반 RBAC
