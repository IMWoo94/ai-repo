# End-User JWT Authentication & Wallet Ownership Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Authenticate end users with a JWT minted from their memberId, protect `/api/v1/wallets/**` with an OAuth2 resource-server filter chain, and enforce per-user wallet ownership in the service layer so an authenticated member can only act on wallets they own.

**Architecture:** Spring Security multi-`SecurityFilterChain` — a public chain for `/api/v1/auth/**`, the existing admin-header chain unchanged, a new JWT-required chain for `/api/v1/wallets/**`, and a permit-all fallback. Wallet ownership is enforced in `WalletAccessPolicy` against the existing `wallet_accounts.member_id` column; the authenticated memberId flows controller → service → policy. No DB migration (schema already has ownership).

**Tech Stack:** Java 25, Spring Boot 4, Spring Security OAuth2 Resource Server (Nimbus, HMAC-SHA256 symmetric key), JUnit 5, React/TypeScript (Vitest + Playwright).

---

## Status (2026-06-06)

**Backend complete (Tasks 1–11)** — committed on `agent/enduser-jwt-auth-wallet-ownership-20260605` (`d29ecf8` for T8–T11; T1–T7 in earlier commits). `./gradlew test` (255), `scenarioTest`, `postgresScenarioTest`, `check` all green. See progress `0066`, ADR-0056.

Deviations from the as-written plan, decided with the maintainer:
- **Enforcement layer:** kept service-core `memberId` threading (Task 8 approach A) over a controller-boundary guard, for service-layer defense-in-depth.
- **IDOR response:** non-owner returns **403 `WALLET_ACCESS_DENIED`** (honest signal), not a 404 collapse — existence-enumeration distinction is an accepted tradeoff.
- **Ledger coverage (added):** `getLedgerEntries` was also threaded + ownership-checked; the original plan did not cover the `/api/v1/wallets/{id}/ledger-entries` IDOR.
- **test-fixtures:** `AdminApiPathMatcher` aligned with the admin chain so fixture POSTs authenticate as ADMIN.

**Pending (Tasks 12–15):** frontend login + Bearer header, frontend E2E auth flow, and the final PR. JWT refresh/expiry policy is also deferred.

---

## File Structure

**Backend — create:**
- `src/main/java/com/imwoo/airepo/wallet/application/AuthTokenService.java` — interface: mint token for memberId
- `src/main/java/com/imwoo/airepo/wallet/application/AuthToken.java` — record (token, memberId, expiresAt)
- `src/main/java/com/imwoo/airepo/wallet/application/MemberNotFoundException.java`
- `src/main/java/com/imwoo/airepo/wallet/application/MemberNotActiveException.java`
- `src/main/java/com/imwoo/airepo/wallet/application/WalletAccessDeniedException.java`
- `src/main/java/com/imwoo/airepo/wallet/application/AuthTokenProperties.java` — secret + ttl binding
- `src/main/java/com/imwoo/airepo/wallet/infra/JwtAuthTokenService.java` — Nimbus JwtEncoder impl
- `src/main/java/com/imwoo/airepo/wallet/api/AuthTokenController.java`
- `src/main/java/com/imwoo/airepo/wallet/api/AuthTokenRequest.java`
- `src/main/java/com/imwoo/airepo/wallet/api/AuthTokenResponse.java`
- `src/main/java/com/imwoo/airepo/wallet/api/WalletPrincipal.java` — helper to read memberId from Jwt

**Backend — modify:**
- `build.gradle` — add oauth2-resource-server + oauth2-jose
- `src/main/resources/application.yml` — add `ai-repo.auth.jwt.secret` + `ttl-minutes`
- `WalletCommandService` / `WalletQueryService` interfaces — add memberId param
- `InMemoryWalletCommandService` / `InMemoryWalletQueryService` — thread memberId + ownership check
- `WalletAccessPolicy` — add `requireOwnership`
- `WalletCommandController` / `WalletQueryController` — read `@AuthenticationPrincipal Jwt`, pass memberId
- `SecurityConfig` — add auth + wallet JWT chains, JwtDecoder bean
- `WalletApiExceptionHandler` — map `WalletAccessDeniedException` → 403, member exceptions

**Frontend — modify:**
- `frontend/src/App.tsx` — login form, token storage, Authorization header, wallet fixed to member
- `frontend/src/App.test.tsx` — auth flow
- `frontend/e2e/wallet-flow.spec.ts` — auth flow

**Docs:** ADR 0058, progress 0069, issue-draft 0068, release notes, wiki draft.

---

## Task 1: Add OAuth2 resource-server dependencies

**Files:**
- Modify: `build.gradle:17-35`

- [ ] **Step 1: Add dependencies**

In `build.gradle` dependencies block, after the existing `spring-boot-starter-security` line add:

```gradle
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
    implementation 'org.springframework.security:spring-security-oauth2-jose'
```

- [ ] **Step 2: Verify it resolves**

Run: `./gradlew compileJava --console=plain`
Expected: BUILD SUCCESSFUL (no version needed — Spring Boot BOM manages it).

- [ ] **Step 3: Commit**

```bash
git add build.gradle
git commit -m "build: add oauth2 resource server for end-user jwt auth"
```

---

## Task 2: Auth config properties + application.yml

**Files:**
- Create: `src/main/java/com/imwoo/airepo/wallet/application/AuthTokenProperties.java`
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: Create AuthTokenProperties**

```java
package com.imwoo.airepo.wallet.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai-repo.auth.jwt")
public record AuthTokenProperties(String secret, long ttlMinutes) {

    public AuthTokenProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("ai-repo.auth.jwt.secret must be at least 32 chars (HMAC-SHA256)");
        }
        if (ttlMinutes <= 0) {
            throw new IllegalArgumentException("ai-repo.auth.jwt.ttl-minutes must be positive");
        }
    }
}
```

- [ ] **Step 2: Register properties + add yml**

In `AiRepoApplication.java` add `@ConfigurationPropertiesScan` if not present (check first; if other `*Properties` are already bound, follow the existing mechanism). Then in `application.yml` under `ai-repo:` add:

```yaml
  auth:
    jwt:
      secret: ${AI_REPO_AUTH_JWT_SECRET:local-dev-jwt-secret-please-change-32b}
      ttl-minutes: ${AI_REPO_AUTH_JWT_TTL_MINUTES:60}
```

(The local default is exactly 32+ chars so construction passes in dev.)

- [ ] **Step 3: Verify binding**

Run: `./gradlew compileJava --console=plain`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/imwoo/airepo/wallet/application/AuthTokenProperties.java src/main/resources/application.yml src/main/java/com/imwoo/airepo/AiRepoApplication.java
git commit -m "feat: add auth jwt config properties"
```

---

## Task 3: Auth domain exceptions + AuthToken record

**Files:**
- Create: `MemberNotFoundException.java`, `MemberNotActiveException.java`, `WalletAccessDeniedException.java`, `AuthToken.java` (all in `application/`)

- [ ] **Step 1: Create the records/exceptions**

`AuthToken.java`:
```java
package com.imwoo.airepo.wallet.application;

import java.time.Instant;
import java.util.Objects;

public record AuthToken(String token, String memberId, Instant expiresAt) {
    public AuthToken {
        Objects.requireNonNull(token, "token must not be null");
        Objects.requireNonNull(memberId, "memberId must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }
}
```

`MemberNotFoundException.java`:
```java
package com.imwoo.airepo.wallet.application;

public class MemberNotFoundException extends RuntimeException {
    public MemberNotFoundException(String memberId) {
        super("Member not found: " + memberId);
    }
}
```

`MemberNotActiveException.java`:
```java
package com.imwoo.airepo.wallet.application;

public class MemberNotActiveException extends RuntimeException {
    public MemberNotActiveException(String memberId) {
        super("Member is not active: " + memberId);
    }
}
```

`WalletAccessDeniedException.java`:
```java
package com.imwoo.airepo.wallet.application;

public class WalletAccessDeniedException extends RuntimeException {
    public WalletAccessDeniedException(String walletId) {
        super("Wallet access denied: " + walletId);
    }
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew compileJava --console=plain`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/imwoo/airepo/wallet/application/AuthToken.java src/main/java/com/imwoo/airepo/wallet/application/MemberNotFoundException.java src/main/java/com/imwoo/airepo/wallet/application/MemberNotActiveException.java src/main/java/com/imwoo/airepo/wallet/application/WalletAccessDeniedException.java
git commit -m "feat: add auth token record and access exceptions"
```

---

## Task 4: AuthTokenService interface + JwtAuthTokenService (TDD)

**Files:**
- Create: `application/AuthTokenService.java`, `infra/JwtAuthTokenService.java`
- Test: `src/test/java/com/imwoo/airepo/wallet/infra/JwtAuthTokenServiceTest.java`

- [ ] **Step 1: Create the interface**

```java
package com.imwoo.airepo.wallet.application;

public interface AuthTokenService {
    AuthToken issueToken(String memberId);
}
```

- [ ] **Step 2: Write the failing test**

`JwtAuthTokenServiceTest.java`:
```java
package com.imwoo.airepo.wallet.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.imwoo.airepo.wallet.application.AuthToken;
import com.imwoo.airepo.wallet.application.AuthTokenProperties;
import com.imwoo.airepo.wallet.application.MemberNotActiveException;
import com.imwoo.airepo.wallet.application.MemberNotFoundException;
import com.imwoo.airepo.wallet.domain.Member;
import com.imwoo.airepo.wallet.domain.MemberStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JwtAuthTokenServiceTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-32b";
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC);

    private JwtAuthTokenService service(MemberStatus status) {
        return new JwtAuthTokenService(
                new AuthTokenProperties(SECRET, 60),
                clock,
                memberId -> status == null
                        ? Optional.empty()
                        : Optional.of(new Member(memberId, status, Instant.parse("2026-05-01T00:00:00Z")))
        );
    }

    @Test
    void issuesSignedTokenForActiveMember() {
        AuthToken token = service(MemberStatus.ACTIVE).issueToken("member-001");

        assertThat(token.memberId()).isEqualTo("member-001");
        assertThat(token.token()).isNotBlank();
        assertThat(token.expiresAt()).isEqualTo(Instant.parse("2026-05-01T01:00:00Z"));
    }

    @Test
    void rejectsUnknownMember() {
        assertThatThrownBy(() -> service(null).issueToken("ghost"))
                .isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    void rejectsInactiveMember() {
        assertThatThrownBy(() -> service(MemberStatus.SUSPENDED).issueToken("member-001"))
                .isInstanceOf(MemberNotActiveException.class);
    }
}
```

NOTE: confirm the actual `MemberStatus` enum constants (`ACTIVE`, and a non-active value) before finalizing — read `MemberStatus.java`. If the non-active constant is not `SUSPENDED`, use the real one.

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew test --tests '*JwtAuthTokenServiceTest' --rerun-tasks --console=plain`
Expected: FAIL — `JwtAuthTokenService` does not exist / does not compile.

- [ ] **Step 4: Implement JwtAuthTokenService**

The service takes a small functional lookup (`MemberLookup`) so it stays infra-light and testable without a full repository. Define it as a nested or standalone interface in `application`:

```java
// application/MemberLookup.java
package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.Member;
import java.util.Optional;

@FunctionalInterface
public interface MemberLookup {
    Optional<Member> findMember(String memberId);
}
```

`infra/JwtAuthTokenService.java`:
```java
package com.imwoo.airepo.wallet.infra;

import com.imwoo.airepo.wallet.application.AuthToken;
import com.imwoo.airepo.wallet.application.AuthTokenProperties;
import com.imwoo.airepo.wallet.application.AuthTokenService;
import com.imwoo.airepo.wallet.application.MemberLookup;
import com.imwoo.airepo.wallet.application.MemberNotActiveException;
import com.imwoo.airepo.wallet.application.MemberNotFoundException;
import com.imwoo.airepo.wallet.domain.Member;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.springframework.stereotype.Service;

@Service
public class JwtAuthTokenService implements AuthTokenService {

    private final AuthTokenProperties properties;
    private final Clock clock;
    private final MemberLookup memberLookup;

    public JwtAuthTokenService(AuthTokenProperties properties, Clock clock, MemberLookup memberLookup) {
        this.properties = properties;
        this.clock = clock;
        this.memberLookup = memberLookup;
    }

    @Override
    public AuthToken issueToken(String memberId) {
        Member member = memberLookup.findMember(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
        if (!member.active()) {
            throw new MemberNotActiveException(memberId);
        }
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(Duration.ofMinutes(properties.ttlMinutes()));
        try {
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    new JWTClaimsSet.Builder()
                            .subject(member.memberId())
                            .issueTime(Date.from(now))
                            .expirationTime(Date.from(expiresAt))
                            .build()
            );
            jwt.sign(new MACSigner(properties.secret().getBytes(StandardCharsets.UTF_8)));
            return new AuthToken(jwt.serialize(), member.memberId(), expiresAt);
        } catch (JOSEException exception) {
            throw new IllegalStateException("Failed to sign auth token", exception);
        }
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew test --tests '*JwtAuthTokenServiceTest' --rerun-tasks --console=plain`
Expected: PASS (3 tests).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/imwoo/airepo/wallet/application/AuthTokenService.java src/main/java/com/imwoo/airepo/wallet/application/MemberLookup.java src/main/java/com/imwoo/airepo/wallet/infra/JwtAuthTokenService.java src/test/java/com/imwoo/airepo/wallet/infra/JwtAuthTokenServiceTest.java
git commit -m "feat: mint signed jwt for active members"
```

---

## Task 5: Wire MemberLookup bean

**Files:**
- Modify: a config class (e.g. create `application/AuthBeansConfig.java` or add to existing config). Check how the existing `Clock` bean and repositories are wired first.

- [ ] **Step 1: Provide MemberLookup from the repository**

The wallet repository already implements `findMember`. Expose a `MemberLookup` bean delegating to `WalletQueryRepository`:

```java
// api/config or wherever existing @Configuration beans live — match the existing pattern
@Bean
MemberLookup memberLookup(WalletQueryRepository walletQueryRepository) {
    return walletQueryRepository::findMember;
}
```

Read the existing config (where `Clock` is defined) and add this bean there.

- [ ] **Step 2: Verify context loads**

Run: `./gradlew test --tests '*ActuatorHealthEndpointTest' --rerun-tasks --console=plain`
Expected: PASS (context wires AuthTokenService).

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat: wire member lookup bean for auth token service"
```

---

## Task 6: AuthTokenController (TDD)

**Files:**
- Create: `api/AuthTokenController.java`, `api/AuthTokenRequest.java`, `api/AuthTokenResponse.java`
- Test: `src/test/java/com/imwoo/airepo/wallet/api/AuthTokenControllerTest.java`

- [ ] **Step 1: Write the failing test**

Use `@WebMvcTest(AuthTokenController.class)` with a mocked `AuthTokenService`. Mirror an existing `@WebMvcTest` test in the repo for exact imports (read one first, e.g. a controller test under `api/`).

```java
package com.imwoo.airepo.wallet.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.imwoo.airepo.wallet.application.AuthToken;
import com.imwoo.airepo.wallet.application.AuthTokenService;
import com.imwoo.airepo.wallet.application.MemberNotFoundException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
// ...standard @WebMvcTest imports per existing controller tests

// @WebMvcTest(AuthTokenController.class) + import the security test config used by other api tests
class AuthTokenControllerTest {

    // @Autowired MockMvc mockMvc;  @MockitoBean AuthTokenService authTokenService;

    @Test
    void issuesTokenForMember() throws Exception {
        when(authTokenService.issueToken("member-001"))
                .thenReturn(new AuthToken("signed.jwt.value", "member-001", Instant.parse("2026-05-01T01:00:00Z")));

        mockMvc.perform(post("/api/v1/auth/tokens")
                        .contentType("application/json")
                        .content("{\"memberId\":\"member-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("signed.jwt.value"))
                .andExpect(jsonPath("$.memberId").value("member-001"));
    }

    @Test
    void returns404ForUnknownMember() throws Exception {
        when(authTokenService.issueToken("ghost")).thenThrow(new MemberNotFoundException("ghost"));

        mockMvc.perform(post("/api/v1/auth/tokens")
                        .contentType("application/json")
                        .content("{\"memberId\":\"ghost\"}"))
                .andExpect(status().isNotFound());
    }
}
```

IMPORTANT: `/api/v1/auth/**` must be permitted in the security chain (Task 9). Until then this `@WebMvcTest` may need the auth chain stubbed. If the existing api tests import a shared test security config, reuse it; otherwise add `.with(...)` per the existing pattern. Verify against a real existing api test before finalizing.

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew test --tests '*AuthTokenControllerTest' --rerun-tasks --console=plain`
Expected: FAIL — controller does not exist.

- [ ] **Step 3: Implement request/response + controller**

`AuthTokenRequest.java`:
```java
package com.imwoo.airepo.wallet.api;

public record AuthTokenRequest(String memberId) {}
```

`AuthTokenResponse.java`:
```java
package com.imwoo.airepo.wallet.api;

import com.imwoo.airepo.wallet.application.AuthToken;
import java.time.Instant;

public record AuthTokenResponse(String token, String memberId, Instant expiresAt) {
    static AuthTokenResponse from(AuthToken token) {
        return new AuthTokenResponse(token.token(), token.memberId(), token.expiresAt());
    }
}
```

`AuthTokenController.java`:
```java
package com.imwoo.airepo.wallet.api;

import com.imwoo.airepo.wallet.application.AuthTokenService;
import com.imwoo.airepo.wallet.application.InvalidWalletOperationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthTokenController {

    private final AuthTokenService authTokenService;

    public AuthTokenController(AuthTokenService authTokenService) {
        this.authTokenService = authTokenService;
    }

    @PostMapping("/tokens")
    public AuthTokenResponse issueToken(@RequestBody AuthTokenRequest request) {
        if (request.memberId() == null || request.memberId().isBlank()) {
            throw new InvalidWalletOperationException("memberId must not be blank");
        }
        return AuthTokenResponse.from(authTokenService.issueToken(request.memberId()));
    }
}
```

- [ ] **Step 4: Map member exceptions in WalletApiExceptionHandler**

Add to `WalletApiExceptionHandler`:
```java
    @ExceptionHandler(MemberNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleMemberNotFound(MemberNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(MemberNotActiveException.class)
    ResponseEntity<ApiErrorResponse> handleMemberNotActive(MemberNotActiveException exception) {
        return error(HttpStatus.CONFLICT, "MEMBER_NOT_ACTIVE", exception.getMessage());
    }
```
(add the matching imports)

- [ ] **Step 5: Run to verify it passes**

Run: `./gradlew test --tests '*AuthTokenControllerTest' --rerun-tasks --console=plain`
Expected: PASS (2 tests).

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: add auth token issuance endpoint"
```

---

## Task 7: WalletAccessPolicy.requireOwnership (TDD)

**Files:**
- Modify: `application/WalletAccessPolicy.java`
- Test: `src/test/java/com/imwoo/airepo/wallet/application/WalletAccessPolicyTest.java` (create)

- [ ] **Step 1: Write the failing test**

`WalletAccessPolicy` is package-private, so the test lives in the same package.
```java
package com.imwoo.airepo.wallet.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.imwoo.airepo.wallet.domain.WalletAccount;
import com.imwoo.airepo.wallet.domain.WalletAccountStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class WalletAccessPolicyTest {

    private WalletAccount wallet(String walletId, String memberId) {
        return new WalletAccount(walletId, memberId, WalletAccountStatus.ACTIVE, Instant.parse("2026-05-01T00:00:00Z"));
    }

    @Test
    void allowsOwnerAccess() {
        assertThatCode(() -> WalletAccessPolicy.requireOwnership(wallet("wallet-001", "member-001"), "member-001"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsNonOwnerAccess() {
        assertThatThrownBy(() -> WalletAccessPolicy.requireOwnership(wallet("wallet-001", "member-001"), "member-002"))
                .isInstanceOf(WalletAccessDeniedException.class);
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew test --tests '*WalletAccessPolicyTest' --rerun-tasks --console=plain`
Expected: FAIL — `requireOwnership` does not exist.

- [ ] **Step 3: Implement requireOwnership**

Add to `WalletAccessPolicy`:
```java
    static void requireOwnership(WalletAccount walletAccount, String memberId) {
        if (!walletAccount.memberId().equals(memberId)) {
            throw new WalletAccessDeniedException(walletAccount.walletId());
        }
    }
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew test --tests '*WalletAccessPolicyTest' --rerun-tasks --console=plain`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/imwoo/airepo/wallet/application/WalletAccessPolicy.java src/test/java/com/imwoo/airepo/wallet/application/WalletAccessPolicyTest.java
git commit -m "feat: add wallet ownership check to access policy"
```

---

## Task 8: Thread memberId through services (TDD)

This is the largest task: change the two service interfaces and both implementations, then update every caller. Do it test-first at the service level, then fix compilation across callers.

**Files:**
- Modify: `WalletCommandService`, `WalletQueryService` (interfaces)
- Modify: `InMemoryWalletCommandService`, `InMemoryWalletQueryService`
- Test: `InMemoryWalletCommandServiceTest`, and any service test asserting ownership

- [ ] **Step 1: Write the failing ownership test**

Add to `InMemoryWalletCommandServiceTest` (fixtures: wallet-001 owned by member-001, wallet-002 by member-002):
```java
    @Test
    void chargeRejectsWhenMemberDoesNotOwnWallet() {
        assertThatThrownBy(() -> service.charge(
                "member-002",
                "wallet-001",
                new WalletChargeCommand(money("5000"), "charge-001", "테스트 충전")))
                .isInstanceOf(WalletAccessDeniedException.class);
    }

    @Test
    void transferRejectsWhenMemberDoesNotOwnSourceWallet() {
        assertThatThrownBy(() -> service.transfer(
                "member-002",
                "wallet-001",
                new WalletTransferCommand("wallet-002", money("1000"), "transfer-001", "테스트 송금")))
                .isInstanceOf(WalletAccessDeniedException.class);
    }
```
Also update ALL existing `service.charge(...)`/`service.transfer(...)` calls in this test file to pass the owning memberId as the first arg (`"member-001"` for wallet-001).

- [ ] **Step 2: Run to verify it fails (compile error first)**

Run: `./gradlew test --tests '*InMemoryWalletCommandServiceTest' --rerun-tasks --console=plain`
Expected: FAIL — method signature mismatch (3 args).

- [ ] **Step 3: Change interfaces**

`WalletCommandService`:
```java
    WalletCommandResult charge(String memberId, String walletId, WalletChargeCommand command);
    WalletCommandResult transfer(String memberId, String sourceWalletId, WalletTransferCommand command);
```
`WalletQueryService`:
```java
    WalletBalance getBalance(String memberId, String walletId);
    List<TransactionHistoryItem> getTransactions(String memberId, String walletId);
```

- [ ] **Step 4: Update InMemoryWalletCommandService**

Add `memberId` param to `charge`/`transfer`. After `findOperableWallet(walletId)` returns the `WalletAccount`, call ownership check. The existing `findOperableWallet` returns the account; capture it and check ownership before applying:
```java
    @Override
    public synchronized WalletCommandResult charge(String memberId, String walletId, WalletChargeCommand command) {
        validateWalletId(walletId);
        validateMoney(command.money());
        validateIdempotencyKey(command.idempotencyKey());

        WalletAccount walletAccount = findOperableWallet(walletId);
        WalletAccessPolicy.requireOwnership(walletAccount, memberId);
        // ... unchanged from here
    }
```
For `transfer`, check ownership on the **source** wallet only:
```java
    @Override
    public synchronized WalletCommandResult transfer(String memberId, String sourceWalletId, WalletTransferCommand command) {
        // ...existing validation...
        WalletAccount sourceWallet = findOperableWallet(sourceWalletId);
        WalletAccessPolicy.requireOwnership(sourceWallet, memberId);
        WalletAccount targetWallet = findOperableWallet(command.targetWalletId());
        // ... unchanged
    }
```

- [ ] **Step 5: Update InMemoryWalletQueryService**

It already resolves the account via `WalletAccessPolicy.findQueryableWallet(...)`, so just add the `memberId` param and one ownership line right after:
```java
    @Override
    public WalletBalance getBalance(String memberId, String walletId) {
        validateWalletId(walletId);
        WalletAccount walletAccount = WalletAccessPolicy.findQueryableWallet(walletQueryRepository, walletId);
        WalletAccessPolicy.requireOwnership(walletAccount, memberId);
        WalletBalance balance = walletQueryRepository.findBalance(walletAccount.walletId())
                .orElseThrow(() -> walletNotFound(walletId));
        return new WalletBalance(balance.walletId(), balance.money(), Instant.now(clock));
    }

    @Override
    public List<TransactionHistoryItem> getTransactions(String memberId, String walletId) {
        validateWalletId(walletId);
        WalletAccount walletAccount = WalletAccessPolicy.findQueryableWallet(walletQueryRepository, walletId);
        WalletAccessPolicy.requireOwnership(walletAccount, memberId);
        return walletQueryRepository.findTransactions(walletAccount.walletId()).stream()
                .sorted(Comparator.comparing(TransactionHistoryItem::occurredAt).reversed())
                .toList();
    }
```

- [ ] **Step 6: Run service test to verify pass**

Run: `./gradlew test --tests '*InMemoryWalletCommandServiceTest' --rerun-tasks --console=plain`
Expected: PASS including the two new ownership-denied tests.

- [ ] **Step 7: Commit (compile of callers happens in Task 9-10)**

```bash
git add -A
git commit -m "feat: enforce wallet ownership in command and query services"
```

---

## Task 9: SecurityConfig — auth + wallet JWT chains (TDD via controller security test)

**Files:**
- Modify: `api/SecurityConfig.java`
- Create: `api/WalletPrincipal.java`

- [ ] **Step 1: Add JwtDecoder + chains**

Read the current `SecurityConfig` (single chain). Refactor to multiple `@Bean SecurityFilterChain` ordered with `@Order`:
- `@Order(1)` auth chain: `securityMatcher("/api/v1/auth/**")` → `permitAll`.
- `@Order(2)` admin chain: existing admin matchers (move the current admin rules here, keep `AdminHeaderAuthenticationFilter`). Use `securityMatcher(...)` listing the operational API paths.
- `@Order(3)` wallet chain: `securityMatcher("/api/v1/wallets/**")` → `.oauth2ResourceServer(o -> o.jwt(...))`, `anyRequest().authenticated()`.
- `@Order(4)` default chain: `anyRequest().permitAll()`.

Add the decoder:
```java
    @Bean
    JwtDecoder walletJwtDecoder(AuthTokenProperties properties) {
        byte[] key = properties.secret().getBytes(StandardCharsets.UTF_8);
        SecretKeySpec secretKey = new SecretKeySpec(key, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }
```

`WalletPrincipal.java` (helper to extract memberId):
```java
package com.imwoo.airepo.wallet.api;

import org.springframework.security.oauth2.jwt.Jwt;

final class WalletPrincipal {
    private WalletPrincipal() {}

    static String memberId(Jwt jwt) {
        return jwt.getSubject();
    }
}
```

NOTE: Because the admin chain previously used `anyRequest()`, splitting into `securityMatcher`-scoped chains changes matching. Verify existing admin tests still pass after this task. The wallet JWT chain must be ordered BEFORE the default permit-all chain.

- [ ] **Step 2: Verify existing admin + actuator tests still pass**

Run: `./gradlew test --tests '*AdminApiPathMatcherTest' --tests '*AdminApiAccessAuditFilterTest' --tests '*ActuatorHealthEndpointTest' --rerun-tasks --console=plain`
Expected: PASS (admin auth unchanged, health still public).

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat: add auth and wallet jwt security filter chains"
```

---

## Task 10: Wallet controllers read JWT subject + ownership end-to-end (TDD)

**Files:**
- Modify: `WalletCommandController`, `WalletQueryController`
- Modify: `WalletApiExceptionHandler` (map `WalletAccessDeniedException` → 403)
- Test: `WalletCommandControllerTest`, `WalletQueryControllerTest` (security cases)

- [ ] **Step 1: Map WalletAccessDeniedException → 403**

Add to `WalletApiExceptionHandler`:
```java
    @ExceptionHandler(WalletAccessDeniedException.class)
    ResponseEntity<ApiErrorResponse> handleWalletAccessDenied(WalletAccessDeniedException exception) {
        return error(HttpStatus.FORBIDDEN, "WALLET_ACCESS_DENIED", exception.getMessage());
    }
```

- [ ] **Step 2: Write failing controller security tests**

Read the existing `WalletCommandControllerTest` for the exact `@WebMvcTest` + security test setup. Add (using `jwt()` request post-processor from `spring-security-test`, `.jwt(jwt -> jwt.subject("member-002"))`):
```java
    @Test
    void chargeWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/wallets/wallet-001/charges")
                        .contentType("application/json")
                        .content(chargeBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void chargeOnUnownedWalletReturns403() throws Exception {
        when(walletCommandService.charge(eq("member-002"), eq("wallet-001"), any()))
                .thenThrow(new WalletAccessDeniedException("wallet-001"));

        mockMvc.perform(post("/api/v1/wallets/wallet-001/charges")
                        .with(jwt().jwt(jwt -> jwt.subject("member-002")))
                        .contentType("application/json")
                        .content(chargeBody()))
                .andExpect(status().isForbidden());
    }
```

- [ ] **Step 3: Run to verify fail**

Run: `./gradlew test --tests '*WalletCommandControllerTest' --rerun-tasks --console=plain`
Expected: FAIL — controller does not yet pass memberId / 401 not enforced.

- [ ] **Step 4: Update controllers to read subject**

`WalletCommandController.charge/transfer` add `@AuthenticationPrincipal Jwt jwt` and pass `WalletPrincipal.memberId(jwt)` as the first service arg. Same for `WalletQueryController`.
```java
    public ResponseEntity<WalletOperationResult> charge(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String walletId,
            @RequestBody WalletChargeRequest request) {
        WalletCommandResult result = walletCommandService.charge(
                WalletPrincipal.memberId(jwt), walletId, /* command */ ...);
        ...
    }
```

- [ ] **Step 5: Run to verify pass**

Run: `./gradlew test --tests '*WalletCommandControllerTest' --tests '*WalletQueryControllerTest' --rerun-tasks --console=plain`
Expected: PASS including 401 and 403 cases.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: enforce jwt auth and ownership on wallet endpoints"
```

---

## Task 11: Fix remaining callers + full backend green

**Files:**
- Modify: any remaining callers of the changed service signatures — scenario tests, `JdbcWalletRepositoryTest` uses `commandService.charge(...)` etc., `PostgresContainerWalletRepositoryTest`, `WalletScenarioFlowTest`, `PostgresWalletScenarioFlowTest`.

- [ ] **Step 1: Compile tests to find all broken callers**

Run: `./gradlew compileTestJava --console=plain`
Expected: FAIL list of every call site missing the memberId arg.

- [ ] **Step 2: Update each caller**

For every `commandService.charge("wallet-001", ...)` add the owning memberId: `commandService.charge("member-001", "wallet-001", ...)`. wallet-001 → member-001, wallet-002 → member-002 (per fixtures). For query service calls add memberId likewise. For transfer, the memberId is the source wallet's owner.

- [ ] **Step 3: Full backend suite green**

Run: `./gradlew check scenarioTest postgresScenarioTest --rerun-tasks --console=plain`
Expected: BUILD SUCCESSFUL, 0 failures.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "test: thread memberId through all wallet service callers"
```

---

## Task 12: Frontend login + auth header (TDD)

**Files:**
- Modify: `frontend/src/App.tsx`, `frontend/src/App.test.tsx`

- [ ] **Step 1: Write failing component test**

Read `App.test.tsx` `setupFetch()` first. Add a test: before login, wallet actions are not shown / a login form asks for memberId; after entering `member-001` and submitting, `POST /api/v1/auth/tokens` is called and subsequent wallet requests include `Authorization: Bearer <token>`.
```ts
it('logs in with memberId and sends bearer token on wallet requests', async () => {
  // mock POST /api/v1/auth/tokens -> { token: 'jwt-xyz', memberId: 'member-001', expiresAt: ... }
  // render, type member-001, submit
  // assert a subsequent balance fetch carried Authorization: Bearer jwt-xyz
});
```

- [ ] **Step 2: Run to verify fail**

Run: `npm --prefix frontend run test`
Expected: FAIL.

- [ ] **Step 3: Implement login + header in App.tsx**

Add memberId login form, store token in `sessionStorage`, include `Authorization: Bearer ${token}` on all wallet fetches, derive walletId from the logged-in member (fixtures: member-001 → wallet-001). Remove the free-form walletId input for user actions (operator console untouched).

- [ ] **Step 4: Run to verify pass**

Run: `npm --prefix frontend run test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/App.tsx frontend/src/App.test.tsx
git commit -m "feat: add member login and bearer auth to wallet ui"
```

---

## Task 13: Frontend E2E auth flow

**Files:**
- Modify: `frontend/e2e/wallet-flow.spec.ts`

- [ ] **Step 1: Update E2E to log in first**

Update the happy-path spec to log in as member-001 before wallet actions; assert balance/charge/transfer work post-login. Keep operator-console specs using admin headers (unchanged). Add an assertion that wallet actions require login.

- [ ] **Step 2: Run E2E locally**

Run: `npm --prefix frontend run e2e`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add frontend/e2e/wallet-flow.spec.ts
git commit -m "test: cover member login in wallet e2e flow"
```

---

## Task 14: Docs sync + dev-rules

**Files:**
- Create: `docs/adr/0058-enduser-jwt-auth-wallet-ownership.md`, `docs/progress/0069-enduser-jwt-auth-wallet-ownership.md`, `issue-drafts/0068-enduser-jwt-auth-wallet-ownership.md`
- Modify: `docs/adr/README.md`, `docs/progress/README.md`, `docs/releases/unreleased.md`, `wiki-drafts/Architecture-Decisions.md`

- [ ] **Step 1: Write ADR 0058**

Follow the existing ADR format (상태/배경/결정 table/트레이드오프/검증 기준/후속 작업). State the security boundary explicitly: password-less token issuance proves authenticated-user isolation (IDOR prevention), not identity spoofing prevention. Note transfer checks source ownership only.

- [ ] **Step 2: Write progress 0069 + issue-draft 0068**

Mirror the format of the latest progress/issue-draft files.

- [ ] **Step 3: Update indexes + release notes + wiki**

Add ADR-0058 to `docs/adr/README.md` and `wiki-drafts/Architecture-Decisions.md` read order; add progress row; add "End-user JWT authentication & wallet ownership" to `docs/releases/unreleased.md` candidate list.

- [ ] **Step 4: Run dev-rules**

Run: `AI_REPO_DEV_RULES_BASE=main scripts/check-dev-rules.sh`
Expected: `PASS`.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "docs: record enduser jwt auth & wallet ownership (#0068)"
```

---

## Task 15: Full verification + PR

- [ ] **Step 1: Full green**

Run: `./gradlew check scenarioTest postgresScenarioTest --rerun-tasks --console=plain && npm --prefix frontend run test && npm --prefix frontend run e2e`
Expected: all PASS.

- [ ] **Step 2: Create issue + push + PR**

```bash
gh issue create --repo IMWoo94/ai-repo --title "feat: end-user JWT authentication & wallet ownership" --label enhancement --body-file issue-drafts/0068-enduser-jwt-auth-wallet-ownership.md
git push -u origin agent/enduser-jwt-auth-wallet-ownership-20260605
gh pr create --repo IMWoo94/ai-repo --base main --title "feat: end-user JWT auth & wallet ownership (#<issue>)" --body "..."
```

---

## Notes on verification commands

This repo suppresses output with caching; always use `--rerun-tasks` and read the `build/test-results/.../*.xml` `failures=`/`errors=` counts to confirm, not just "BUILD SUCCESSFUL".
