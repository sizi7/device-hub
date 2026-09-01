# DeviceHub Development Rules

- 기존 함수 이름을 임의로 변경하지 않는다.
- 기존 클래스명과 변수명을 임의로 변경하지 않는다.
- 기존 API path를 임의로 변경하지 않는다.
- 정상 동작하는 기존 기능을 불필요하게 리팩터링하지 않는다.
- 변경 전 기존 구현을 먼저 확인한다.
- 기존 Backend/Frontend 기능과 호환성을 유지한다.
- 작업 완료 후 Backend와 Frontend build를 검증한다.

## 빌드 검증 명령

```powershell
.\gradlew.bat build
cd frontend; npm run build
```

## 참고

단계별 구현 범위와 학습 기록 규칙은 `AGENTS.md`를 따른다.
