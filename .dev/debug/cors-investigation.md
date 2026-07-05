# CORS 의심 증상 코드 레벨 조사

## 증상 기록

- 사용자 표현: "`course`라는 게 뜨면서 돌아가지 않는 케이스"
- 코드 검색 결과: 의미 있는 `course` 문자열은 없음. 브라우저 콘솔의 `CORS` 오류를 `course`로 표현했을 가능성이 높음.
- 재현 조건: 아직 미확정. 어떤 URL로 프론트를 열었는지, 어떤 명령으로 실행했는지, 콘솔 원문은 확인 전.
- 조사 범위: 코드, 프론트 실행 설정, 테스트 실행 경로, 관련 문서만 확인. 런타임 재현과 수정은 수행하지 않음.

## 결론

가장 강한 코드 레벨 의심점은 "로컬 개발 경로는 Vite proxy에 의존하지만, 백엔드에는 브라우저 cross-origin 요청을 허용하는 CORS 설정이 없다"는 구조다.

이 구조에서는 `http://localhost:5173` 또는 `http://127.0.0.1:5173`에서 Vite dev server를 통해 `/api` 상대 경로로 호출할 때는 정상 동작할 수 있다. 반대로 프론트를 `vite preview`, 정적 파일, 다른 포트, 다른 호스트, 배포된 별도 origin에서 열고 백엔드 API를 직접 호출하면 브라우저 CORS/preflight 단계에서 막힐 수 있다.

## 근거

1. 백엔드 Security 설정에 CORS 활성화가 없다.
   - `src/main/java/com/imwoo/airepo/wallet/api/SecurityConfig.java:34` auth API filter chain
   - `src/main/java/com/imwoo/airepo/wallet/api/SecurityConfig.java:48` admin API filter chain
   - `src/main/java/com/imwoo/airepo/wallet/api/SecurityConfig.java:86` wallet API filter chain
   - `src/main/java/com/imwoo/airepo/wallet/api/SecurityConfig.java:101` default filter chain
   - 각 chain은 `csrf`, `httpBasic`, `formLogin`, `logout` 등을 비활성화하지만 `.cors(...)` 또는 CORS bean 연결이 없다.

2. CORS 관련 코드/설정이 검색되지 않았다.
   - 검색어: `@CrossOrigin`, `CorsConfiguration`, `CorsFilter`, `cors(`, `addCorsMappings`, `allowedOrigins`, `Access-Control`
   - 대상: `src/main/java`, `src/main/resources`, `frontend/src`, `frontend/*.ts`, `frontend/*.json`, 관련 docs
   - 의미 있는 매치 없음.

3. 프론트는 API base URL 설정 없이 상대 경로를 호출한다.
   - `frontend/src/App.tsx:153` `fetch(url)` wrapper
   - `frontend/src/App.tsx:287` `/api/v1/auth/tokens`
   - `frontend/src/App.tsx:296` wallet 요청도 전달받은 상대 URL을 `fetch(url)`로 호출

4. 로컬 개발 프록시는 Vite dev server에만 명시되어 있다.
   - `frontend/vite.config.ts:6` `server.proxy`
   - `frontend/vite.config.ts:8` `/api` 프록시
   - `frontend/vite.config.ts:9` target `http://localhost:8080`
   - `frontend/package.json:7` dev server는 `5173`
   - `frontend/package.json:13` preview server는 `4173`

5. 문서도 Vite proxy 경로를 전제로 한다.
   - `docs/frontend/react-user-frontend.md:31` 브라우저에서 `http://localhost:5173`을 열고 Vite proxy가 `/api`를 Spring Boot `8080`으로 전달한다고 안내

6. E2E 테스트는 이 문제를 가릴 수 있다.
   - `frontend/playwright.config.ts:13` 브라우저 baseURL은 `http://127.0.0.1:5173`
   - `frontend/playwright.config.ts:29` E2E가 `npm run dev`를 실행해 Vite proxy 경로를 사용
   - 따라서 "프록시 없이 다른 origin에서 백엔드를 직접 호출하는 브라우저 상황"은 현재 E2E 경로로 검증되지 않는다.

## 패턴 판정

- Configuration Drift: 8/10
  - 문서와 테스트는 Vite proxy 기반 로컬 실행을 전제하지만, 다른 실행 방식에서는 백엔드 CORS 정책 부재가 드러난다.
- Integration Failure: 7/10
  - 프론트와 백엔드 origin 통합 계약이 코드상 명시적으로 고정되어 있지 않다.
- Race Condition, State Corruption, Nil Propagation: 낮음
  - 현재 증상 표현과 코드 정황상 브라우저 네트워크/보안 경계 문제가 더 그럴듯하다.

## 확인 질문

1. 오류가 뜨는 화면을 어떤 주소로 열었는가?
   - 예: `http://localhost:5173`, `http://127.0.0.1:5173`, `http://localhost:4173`, `http://localhost:8080`, `file://...`
2. 프론트 실행 명령은 무엇인가?
   - 예: `npm run dev`, `npm run preview`, 빌드 산출물 직접 열기
3. 브라우저 콘솔 원문에 다음 문구가 포함되는가?
   - `blocked by CORS policy`
   - `No 'Access-Control-Allow-Origin' header`
   - `Response to preflight request doesn't pass access control check`

## 다음 단계 후보

재현 정보가 위 결론과 일치하면 `/bugfix` 단계로 넘길 수 있다. 핸드오프 진단문은 다음과 같다.

> 프론트/백엔드가 다른 origin으로 실행되는 경로에서 백엔드 CORS 정책이 정의되어 있지 않아 브라우저 preflight 또는 cross-origin fetch가 실패하는 것으로 의심된다. 현재 정상 경로는 Vite dev server의 `/api` proxy에 의존하며, E2E도 그 경로만 검증한다.
