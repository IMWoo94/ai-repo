# MVP Release Checklist

이 문서는 `unreleased` 후보를 실제 1차 MVP release로 승격하기 직전 확인할 체크리스트다.

## 버전 정책

| 항목 | 기준 |
| --- | --- |
| 후보 문서 | `docs/releases/unreleased.md` |
| 다음 release note | `docs/releases/v0.7.0.md` |
| 다음 Git tag | `v0.7.0` |
| Gradle version | release PR에서 `0.7.0`으로 bump |
| Release 성격 | 1차 MVP 시연 가능 기준선 |

`v0.6.0`은 outbox/manual review 기준선이고, `v0.7.0`은 React 사용자 화면, 운영자 콘솔, 보안/관측, PostgreSQL scenario CI, local smoke까지 포함한 1차 MVP 기준선으로 둔다.

## Release PR 전 체크

- [ ] `docs/releases/unreleased.md`를 `docs/releases/v0.7.0.md`로 복사하고 실제 포함 범위를 확정한다.
- [ ] `build.gradle`의 `version`을 `0.7.0`으로 변경한다.
- [ ] README의 현재 릴리스 노트 링크에 `v0.7.0`을 추가한다.
- [ ] `docs/progress/README.md`의 현재 릴리스 후보를 `v0.7.0`으로 갱신한다.
- [ ] `wiki-drafts/Release-Notes.md`의 현재 후보를 `v0.7.0` 기준으로 갱신한다.

## 필수 검증 명령

Release PR에는 다음 명령 결과를 첨부한다.

```bash
./gradlew check
./gradlew scenarioTest
./gradlew postgresScenarioTest
npm --prefix frontend run test
npm --prefix frontend run build
npm --prefix frontend run e2e
scripts/mvp-local-smoke.sh
scripts/sync-wiki-drafts.sh wiki-drafts <wiki-checkout>
docker compose config
git diff --check
```

## GitHub 상태 확인

- [ ] Release PR CI가 모두 통과한다.
- [ ] `scripts/sync-wiki-drafts.sh` 결과를 GitHub Wiki checkout에 commit/push한다.
- [ ] Wiki `Home`, `Release-Notes`, `QA-Scenarios`, `Architecture-Decisions`가 GitHub에서 열리는지 확인한다.
- [ ] GitHub Release body에는 포함 기능, 검증 결과, 알려진 제약, 다음 후보를 기록한다.

## 알려진 제약 명시

Release note에는 다음 제약을 숨기지 않고 기록한다.

- 운영자 relay health/pruning 화면은 아직 없다.
- Kafka/RabbitMQ/SQS adapter는 아직 없다.
- consumer idempotency는 아직 없다.
- operator/admin token 분리와 실제 로그인 연동은 아직 없다.
- external alert channel과 승인 워크플로우는 아직 없다.
- manual review requeue full E2E fixture는 아직 없다.

## Tag 발행 순서

1. Release PR을 merge한다.
2. 로컬 `main`을 `origin/main`에 fast-forward 한다.
3. `git tag -a v0.7.0 -m "v0.7.0"`을 생성한다.
4. `git push origin v0.7.0`을 실행한다.
5. `gh release create v0.7.0 --title "v0.7.0" --notes-file docs/releases/v0.7.0.md`로 GitHub Release를 생성한다.

## Rollback 기준

- Release PR CI가 실패하면 tag를 만들지 않는다.
- local smoke가 실패하면 tag를 만들지 않는다.
- Wiki push가 실패하면 GitHub Release note에 Wiki 미반영 상태를 명시하거나 release를 보류한다.
