# ADR-0049: Consumer Duplicate Spike Alert

## 상태

Accepted

## 배경

Consumer monitoring API는 processed count, duplicate count, receipt count를 보여준다. 하지만 운영자가 숫자를 직접 해석해야 하면 이상 징후 대응이 늦어진다.

금융/핀테크 운영에서는 “중복 메시지가 안전하게 no-op 처리된다”와 별개로, duplicate delivery가 급증하면 broker replay, relay 재시도, consumer 장애 복구 과정의 이상 신호로 봐야 한다.

## 결정

Consumer health summary API를 추가해 duplicate delivery rate 기준으로 `OK`, `WARNING`, `CRITICAL`, `NO_DATA`를 판정한다.

| 항목 | 결정 |
| --- | --- |
| API | `GET /api/v1/outbox-consumer/health` |
| 권한 | 조회성 운영 API이므로 `ROLE_OPERATOR` 이상 |
| 기본 warning 기준 | duplicate event count 5건 이상이고 duplicate rate 20% 이상 |
| 기본 critical 기준 | duplicate event count 5건 이상이고 duplicate rate 50% 이상 |
| no data | processed + duplicate delivery count가 0 |

## 트레이드오프

### 장점

- 운영자가 duplicate count를 직접 해석하지 않아도 상태를 즉시 확인할 수 있다.
- threshold를 환경 설정으로 조정할 수 있다.
- broker-specific adapter 도입 전에도 duplicate delivery 이상 징후 판정 계약을 고정한다.

### 비용

- 현재 판정은 누적 metric 기반이다. 정확한 “최근 N분 spike” 판정은 duplicate attempt history 또는 time bucket metric이 필요하다.
- 낮은 traffic에서는 duplicate 1건이 rate를 크게 흔들 수 있어 `minDuplicateEventCount`로 노이즈를 줄인다.
- 외부 alert channel은 아직 없다.

## 검증 기준

- 데이터가 없으면 `NO_DATA`를 반환한다.
- duplicate rate가 warning 기준에 도달하면 `WARNING`을 반환한다.
- duplicate rate가 critical 기준에 도달하면 `CRITICAL`을 반환한다.
- health API는 operator token과 operator id로 조회할 수 있다.

## 후속 작업

- duplicate attempt history 또는 time bucket metric을 추가해 최근 window 기준 spike를 계산한다.
- external alert channel을 연결한다.
- broker-specific adapter별 duplicate baseline을 문서화한다.
