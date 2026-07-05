# ADR-0068: Deployed Profile Persistence Guard

## 상태

Accepted

## 배경

`DeployedProfiles.isActive`는 `prod`와 `postgres`를 모두 배포 런타임으로 취급한다(자격증명 fail-fast의 판정 축, ADR-0062). 그런데 영속성 선택은 오직 `postgres` 프로파일에만 반응한다.

- `JdbcWalletRepository`는 `@Profile("postgres")`
- `InMemoryWalletRepository`는 `@Profile("!postgres")`

따라서 `prod` 단독 기동(예: `postgres` 프로파일 누락)은 `JwtSecretGuard`/`OpsTokenGuard`/`BrokerTokenGuard` 같은 자격증명 가드는 통과하지만, 영속성은 `!postgres`로 판정되어 `InMemoryWalletRepository`가 로드된다. 결과적으로 배포로 인식된 런타임이 in-memory 저장소로 뜨고, 재시작 시 지갑 잔액이 유실된다(#145).

## 결정

`DeployedProfilePersistenceGuard`를 신설한다. 기존 자격증명 가드(`JwtSecretGuard` 등)와 동일한 패턴이다.

| 항목 | 결정 |
| --- | --- |
| 위치 | `wallet/config` 패키지의 package-private `@Component` |
| 판정 | 생성자에서 `Environment` 주입. `DeployedProfiles.isActive(env)`가 true인데 활성 프로파일에 `postgres`가 없으면 startup 실패 |
| 실패 방식 | `IllegalStateException`("deployed profile requires the 'postgres' profile for JDBC persistence; in-memory wallet store must not run in a deployed profile") 을 던져 context refresh 단계에서 기동을 막음 |
| 그 외 | 비배포(local/test/no profile) 또는 `postgres` 포함 시 no-op(`postgres` 포함 시 log.info) |

이로써 "배포 프로파일 = JDBC 영속성" 불변식을 명시적 기동 실패로 강제한다. 콜사이트 변경은 없다(가드 bean 추가 1건).

## 트레이드오프

### 장점

- 콜사이트 변경 0에 가깝다. 기존 `@Profile` 배선을 건드리지 않고 가드 bean 하나로 불변식을 강제한다.
- 데이터 유실이라는 조용한 실패를 기동 시점의 명시적 실패로 앞당긴다(fail-fast).
- 기존 자격증명 가드(`JwtSecretGuard`/`OpsTokenGuard`/`BrokerTokenGuard`)와 동일한 축을 재사용해 일관적이다.

### 비용

- 배포 프로파일 판정과 영속성 프로파일 판정이 여전히 두 곳(`DeployedProfiles`와 `@Profile`)에 분산되어 있고, 가드는 그 정합성을 런타임에 확인할 뿐 구조적으로 통일하지는 않는다.

## 대안

- **`InMemoryWalletRepository`의 `@Profile`을 `!postgres & !prod`로 좁히기**: 배포 프로파일에서 in-memory bean 자체가 등록되지 않게 한다. 다만 이 경우 어떤 wallet repository도 없어 bean 조회 실패로 기동이 깨지되, 메시지가 "no qualifying bean" 수준이라 원인(배포 프로파일에 postgres 누락)이 드러나지 않는다. `prod` 외 다른 배포 프로파일이 추가되면 술어를 계속 늘려야 한다.
- **`DeployedProfiles` 술어를 영속성 기준으로 통일**: 배포 판정을 `postgres`로 좁혀 자격증명과 영속성을 한 축으로 묶는다. 하지만 `prod`를 배포로 취급하는 기존 자격증명 정책(ADR-0062)의 의미가 바뀌어 회귀 범위가 넓다.
- 채택안(가드)은 콜사이트 변경이 거의 없고 원인을 담은 명시적 실패 메시지를 준다는 점에서 위 두 대안보다 외과적이다.
