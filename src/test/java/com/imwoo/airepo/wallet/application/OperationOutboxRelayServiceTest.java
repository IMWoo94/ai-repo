package com.imwoo.airepo.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.imwoo.airepo.wallet.domain.Money;
import com.imwoo.airepo.wallet.domain.OperationOutboxEvent;
import com.imwoo.airepo.wallet.domain.OperationOutboxRequeueRequestStatus;
import com.imwoo.airepo.wallet.domain.OperationOutboxStatus;
import com.imwoo.airepo.wallet.infra.InMemoryOperationOutboxPublisher;
import com.imwoo.airepo.wallet.infra.InMemoryWalletRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class OperationOutboxRelayServiceTest {

    private final InMemoryWalletRepository repository = new InMemoryWalletRepository();
    private final InMemoryWalletCommandService commandService = new InMemoryWalletCommandService(
            Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC),
            repository
    );
    private final InMemoryOperationOutboxPublisher publisher = new InMemoryOperationOutboxPublisher();
    private final OperationOutboxRelayService relayService = new OperationOutboxRelayService(
            Clock.fixed(Instant.parse("2026-05-01T00:01:00Z"), ZoneOffset.UTC),
            repository,
            publisher
    );

    @Test
    void returnsPendingEventsWithLimit() {
        commandService.charge("member-001", "wallet-001", new WalletChargeCommand(money("5000"), "charge-001", "테스트 충전"));
        commandService.transfer(
                "member-001",
                "wallet-001",
                new WalletTransferCommand("wallet-002", money("1000"), "transfer-001", "테스트 송금")
        );

        assertThat(relayService.getPendingEvents(1))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.outboxEventId()).isEqualTo("outbox-001");
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.PENDING);
                    assertThat(outboxEvent.attemptCount()).isZero();
                    assertThat(outboxEvent.nextRetryAt()).isNull();
                    assertThat(outboxEvent.claimedAt()).isNull();
                    assertThat(outboxEvent.leaseExpiresAt()).isNull();
                    assertThat(outboxEvent.publishedAt()).isNull();
                    assertThat(outboxEvent.lastError()).isNull();
                });
    }

    @Test
    void claimsPendingEventsAsProcessingWithLimit() {
        commandService.charge("member-001", "wallet-001", new WalletChargeCommand(money("5000"), "charge-001", "테스트 충전"));
        commandService.transfer(
                "member-001",
                "wallet-001",
                new WalletTransferCommand("wallet-002", money("1000"), "transfer-001", "테스트 송금")
        );

        assertThat(relayService.claimReadyEvents(1))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.outboxEventId()).isEqualTo("outbox-001");
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.PROCESSING);
                    assertThat(outboxEvent.nextRetryAt()).isNull();
                    assertThat(outboxEvent.claimedAt()).isEqualTo(Instant.parse("2026-05-01T00:01:00Z"));
                    assertThat(outboxEvent.leaseExpiresAt()).isEqualTo(Instant.parse("2026-05-01T00:02:00Z"));
                    assertThat(outboxEvent.publishedAt()).isNull();
                    assertThat(outboxEvent.lastError()).isNull();
                });
        assertThat(relayService.getPendingEvents(10))
                .singleElement()
                .satisfies(outboxEvent -> assertThat(outboxEvent.outboxEventId()).isEqualTo("outbox-002"));
    }

    @Test
    void marksEventPublished() {
        commandService.charge("member-001", "wallet-001", new WalletChargeCommand(money("5000"), "charge-001", "테스트 충전"));

        relayService.markPublished("outbox-001");

        assertThat(repository.findOperationOutboxEvents("op-001"))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.PUBLISHED);
                    assertThat(outboxEvent.publishedAt()).isEqualTo(Instant.parse("2026-05-01T00:01:00Z"));
                    assertThat(outboxEvent.attemptCount()).isZero();
                    assertThat(outboxEvent.nextRetryAt()).isNull();
                    assertThat(outboxEvent.claimedAt()).isNull();
                    assertThat(outboxEvent.leaseExpiresAt()).isNull();
                    assertThat(outboxEvent.lastError()).isNull();
                });
        assertThat(relayService.getPendingEvents(10)).isEmpty();
    }

    @Test
    void marksEventFailedWithAttemptCountAndLastError() {
        commandService.charge("member-001", "wallet-001", new WalletChargeCommand(money("5000"), "charge-001", "테스트 충전"));

        relayService.markFailed("outbox-001", "broker unavailable");

        assertThat(repository.findOperationOutboxEvents("op-001"))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.FAILED);
                    assertThat(outboxEvent.attemptCount()).isEqualTo(1);
                    assertThat(outboxEvent.nextRetryAt()).isEqualTo(Instant.parse("2026-05-01T00:01:30Z"));
                    assertThat(outboxEvent.claimedAt()).isNull();
                    assertThat(outboxEvent.leaseExpiresAt()).isNull();
                    assertThat(outboxEvent.lastError()).isEqualTo("broker unavailable");
                    assertThat(outboxEvent.publishedAt()).isNull();
                });
        assertThat(relayService.getPendingEvents(10)).isEmpty();
    }

    @Test
    void claimsFailedEventOnlyAfterNextRetryAt() {
        commandService.charge("member-001", "wallet-001", new WalletChargeCommand(money("5000"), "charge-001", "테스트 충전"));
        relayService.markFailed("outbox-001", "broker unavailable");

        assertThat(relayService.claimReadyEvents(10)).isEmpty();

        OperationOutboxRelayService retryReadyRelayService = new OperationOutboxRelayService(
                Clock.fixed(Instant.parse("2026-05-01T00:01:31Z"), ZoneOffset.UTC),
                repository,
                publisher
        );
        assertThat(retryReadyRelayService.claimReadyEvents(10))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.PROCESSING);
                    assertThat(outboxEvent.nextRetryAt()).isNull();
                    assertThat(outboxEvent.claimedAt()).isEqualTo(Instant.parse("2026-05-01T00:01:31Z"));
                    assertThat(outboxEvent.leaseExpiresAt()).isEqualTo(Instant.parse("2026-05-01T00:02:31Z"));
                    assertThat(outboxEvent.lastError()).isNull();
                });
    }

    @Test
    void claimsProcessingEventAgainAfterLeaseExpires() {
        commandService.charge("member-001", "wallet-001", new WalletChargeCommand(money("5000"), "charge-001", "테스트 충전"));

        relayService.claimReadyEvents(10);
        assertThat(relayService.claimReadyEvents(10)).isEmpty();

        OperationOutboxRelayService leaseExpiredRelayService = new OperationOutboxRelayService(
                Clock.fixed(Instant.parse("2026-05-01T00:02:00Z"), ZoneOffset.UTC),
                repository,
                publisher
        );
        assertThat(leaseExpiredRelayService.claimReadyEvents(10))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.PROCESSING);
                    assertThat(outboxEvent.claimedAt()).isEqualTo(Instant.parse("2026-05-01T00:02:00Z"));
                    assertThat(outboxEvent.leaseExpiresAt()).isEqualTo(Instant.parse("2026-05-01T00:03:00Z"));
                });
    }

    @Test
    void movesEventToManualReviewAfterMaxAttempts() {
        commandService.charge("member-001", "wallet-001", new WalletChargeCommand(money("5000"), "charge-001", "테스트 충전"));
        relayService.markFailed("outbox-001", "broker unavailable");

        OperationOutboxRelayService secondAttemptRelayService = new OperationOutboxRelayService(
                Clock.fixed(Instant.parse("2026-05-01T00:01:31Z"), ZoneOffset.UTC),
                repository,
                publisher
        );
        secondAttemptRelayService.claimReadyEvents(10);
        secondAttemptRelayService.markFailed("outbox-001", "broker unavailable");

        OperationOutboxRelayService thirdAttemptRelayService = new OperationOutboxRelayService(
                Clock.fixed(Instant.parse("2026-05-01T00:02:02Z"), ZoneOffset.UTC),
                repository,
                publisher
        );
        thirdAttemptRelayService.claimReadyEvents(10);
        thirdAttemptRelayService.markFailed("outbox-001", "broker unavailable");

        assertThat(repository.findOperationOutboxEvents("op-001"))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.MANUAL_REVIEW);
                    assertThat(outboxEvent.attemptCount()).isEqualTo(3);
                    assertThat(outboxEvent.nextRetryAt()).isNull();
                    assertThat(outboxEvent.claimedAt()).isNull();
                    assertThat(outboxEvent.leaseExpiresAt()).isNull();
                    assertThat(outboxEvent.lastError()).isEqualTo("broker unavailable");
                });

        OperationOutboxRelayService laterRelayService = new OperationOutboxRelayService(
                Clock.fixed(Instant.parse("2026-05-01T00:10:00Z"), ZoneOffset.UTC),
                repository,
                publisher
        );
        assertThat(laterRelayService.claimReadyEvents(10)).isEmpty();
    }

    @Test
    void publishesReadyEventsAndMarksThemPublished() {
        commandService.charge("member-001", "wallet-001", new WalletChargeCommand(money("5000"), "charge-001", "테스트 충전"));
        commandService.transfer(
                "member-001",
                "wallet-001",
                new WalletTransferCommand("wallet-002", money("1000"), "transfer-001", "테스트 송금")
        );

        OperationOutboxPublishBatchResult result = relayService.publishReadyEvents(10);

        assertThat(result.claimedCount()).isEqualTo(2);
        assertThat(result.publishedCount()).isEqualTo(2);
        assertThat(result.failedCount()).isZero();
        assertThat(publisher.publishedEvents())
                .extracting(OperationOutboxEvent::outboxEventId)
                .containsExactly("outbox-001", "outbox-002");
        assertThat(repository.findOperationOutboxEvents("op-001"))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.PUBLISHED);
                    assertThat(outboxEvent.publishedAt()).isEqualTo(Instant.parse("2026-05-01T00:01:00Z"));
                });
        assertThat(repository.findOperationOutboxEvents("op-002"))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.PUBLISHED);
                    assertThat(outboxEvent.publishedAt()).isEqualTo(Instant.parse("2026-05-01T00:01:00Z"));
                });
    }

    @Test
    void marksPublishFailureAsFailedAndKeepsSuccessfulEventsPublished() {
        commandService.charge("member-001", "wallet-001", new WalletChargeCommand(money("5000"), "charge-001", "테스트 충전"));
        commandService.transfer(
                "member-001",
                "wallet-001",
                new WalletTransferCommand("wallet-002", money("1000"), "transfer-001", "테스트 송금")
        );
        OperationOutboxPublisher partiallyFailingPublisher = outboxEvent -> {
            if ("outbox-002".equals(outboxEvent.outboxEventId())) {
                throw new IllegalStateException("broker unavailable");
            }
        };
        OperationOutboxRelayService failingRelayService = new OperationOutboxRelayService(
                Clock.fixed(Instant.parse("2026-05-01T00:01:00Z"), ZoneOffset.UTC),
                repository,
                partiallyFailingPublisher
        );

        OperationOutboxPublishBatchResult result = failingRelayService.publishReadyEvents(10);

        assertThat(result.claimedCount()).isEqualTo(2);
        assertThat(result.publishedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(repository.findOperationOutboxEvents("op-001"))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.PUBLISHED);
                    assertThat(outboxEvent.lastError()).isNull();
                });
        assertThat(repository.findOperationOutboxEvents("op-002"))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.FAILED);
                    assertThat(outboxEvent.attemptCount()).isEqualTo(1);
                    assertThat(outboxEvent.nextRetryAt()).isEqualTo(Instant.parse("2026-05-01T00:01:30Z"));
                    assertThat(outboxEvent.lastError()).isEqualTo("broker unavailable");
                });
    }

    @Test
    void rejectsStalePublisherAfterLeaseRecovery() {
        commandService.charge("member-001", "wallet-001", new WalletChargeCommand(money("5000"), "charge-001", "테스트 충전"));
        OperationOutboxPublisher slowPublisher = outboxEvent -> {
            OperationOutboxRelayService laterRelayService = new OperationOutboxRelayService(
                    Clock.fixed(Instant.parse("2026-05-01T00:02:00Z"), ZoneOffset.UTC),
                    repository,
                    publisher
            );
            assertThat(laterRelayService.claimReadyEvents(10))
                    .singleElement()
                    .satisfies(reclaimedEvent -> {
                        assertThat(reclaimedEvent.outboxEventId()).isEqualTo(outboxEvent.outboxEventId());
                        assertThat(reclaimedEvent.claimedAt()).isEqualTo(Instant.parse("2026-05-01T00:02:00Z"));
                    });
        };
        OperationOutboxRelayService slowRelayService = new OperationOutboxRelayService(
                Clock.fixed(Instant.parse("2026-05-01T00:01:00Z"), ZoneOffset.UTC),
                repository,
                slowPublisher
        );

        assertThatThrownBy(() -> slowRelayService.publishReadyEvents(10))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("outbox event claim is no longer active: outbox-001");
        assertThat(repository.findOperationOutboxEvents("op-001"))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.PROCESSING);
                    assertThat(outboxEvent.claimedAt()).isEqualTo(Instant.parse("2026-05-01T00:02:00Z"));
                    assertThat(outboxEvent.publishedAt()).isNull();
                });
    }

    @Test
    void returnsAndRequeuesManualReviewEvents() {
        commandService.charge("member-001", "wallet-001", new WalletChargeCommand(money("5000"), "charge-001", "테스트 충전"));
        relayService.markFailed("outbox-001", "broker unavailable");
        relayService.markFailed("outbox-001", "broker unavailable");
        relayService.markFailed("outbox-001", "broker unavailable");

        assertThat(relayService.getManualReviewEvents(10))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.MANUAL_REVIEW);
                    assertThat(outboxEvent.attemptCount()).isEqualTo(3);
                });

        relayService.requeueManualReviewEvent("outbox-001", "ops-user", "broker recovered");

        assertThat(relayService.getManualReviewEvents(10)).isEmpty();
        assertThat(relayService.getRequeueAudits("outbox-001"))
                .singleElement()
                .satisfies(audit -> {
                    assertThat(audit.outboxEventId()).isEqualTo("outbox-001");
                    assertThat(audit.operationId()).isEqualTo("op-001");
                    assertThat(audit.requeuedAt()).isEqualTo(Instant.parse("2026-05-01T00:01:00Z"));
                    assertThat(audit.operator()).isEqualTo("ops-user");
                    assertThat(audit.reason()).isEqualTo("broker recovered");
                });
        assertThat(repository.findOperationOutboxEvents("op-001"))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.PENDING);
                    assertThat(outboxEvent.attemptCount()).isZero();
                    assertThat(outboxEvent.nextRetryAt()).isNull();
                    assertThat(outboxEvent.lastError()).isNull();
                });
        assertThat(relayService.claimReadyEvents(10))
                .singleElement()
                .satisfies(outboxEvent -> assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.PROCESSING));
    }

    @Test
    void requestsApprovesAndExecutesManualReviewRequeue() {
        commandService.charge("member-001", "wallet-001", new WalletChargeCommand(money("5000"), "charge-001", "테스트 충전"));
        relayService.markFailed("outbox-001", "broker unavailable");
        relayService.markFailed("outbox-001", "broker unavailable");
        relayService.markFailed("outbox-001", "broker unavailable");

        var requested = relayService.requestManualReviewRequeue("outbox-001", "ops-requester", "broker recovered");

        assertThat(requested.requestId()).isEqualTo("outbox-requeue-request-001");
        assertThat(requested.status()).isEqualTo(OperationOutboxRequeueRequestStatus.REQUESTED);
        assertThat(requested.requestedBy()).isEqualTo("ops-requester");
        assertThat(requested.requestReason()).isEqualTo("broker recovered");

        var approved = relayService.approveManualReviewRequeueRequest(
                requested.requestId(),
                "ops-approver",
                "원인 조치 확인"
        );

        assertThat(approved.status()).isEqualTo(OperationOutboxRequeueRequestStatus.APPROVED);
        assertThat(approved.approvedBy()).isEqualTo("ops-approver");
        assertThat(approved.approvalReason()).isEqualTo("원인 조치 확인");

        var executed = relayService.executeManualReviewRequeueRequest(requested.requestId(), "ops-executor");

        assertThat(executed.status()).isEqualTo(OperationOutboxRequeueRequestStatus.EXECUTED);
        assertThat(executed.executedBy()).isEqualTo("ops-executor");
        assertThat(relayService.getRequeueRequests("outbox-001"))
                .singleElement()
                .satisfies(request -> assertThat(request.status()).isEqualTo(OperationOutboxRequeueRequestStatus.EXECUTED));
        assertThat(relayService.getRequeueAudits("outbox-001"))
                .singleElement()
                .satisfies(audit -> {
                    assertThat(audit.operator()).isEqualTo("ops-executor");
                    assertThat(audit.reason()).isEqualTo("broker recovered");
                });
        assertThat(repository.findOperationOutboxEvents("op-001"))
                .singleElement()
                .satisfies(outboxEvent -> assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.PENDING));
    }

    @Test
    void rejectsSameRequesterAndApprover() {
        commandService.charge("member-001", "wallet-001", new WalletChargeCommand(money("5000"), "charge-001", "테스트 충전"));
        relayService.markFailed("outbox-001", "broker unavailable");
        relayService.markFailed("outbox-001", "broker unavailable");
        relayService.markFailed("outbox-001", "broker unavailable");
        var requested = relayService.requestManualReviewRequeue("outbox-001", "ops-user", "broker recovered");

        assertThatThrownBy(() -> relayService.approveManualReviewRequeueRequest(
                requested.requestId(),
                "ops-user",
                "self approval"
        ))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("approver must be different from requester");
    }

    @Test
    void rejectsManualReviewRequeueRequest() {
        commandService.charge("member-001", "wallet-001", new WalletChargeCommand(money("5000"), "charge-001", "테스트 충전"));
        relayService.markFailed("outbox-001", "broker unavailable");
        relayService.markFailed("outbox-001", "broker unavailable");
        relayService.markFailed("outbox-001", "broker unavailable");
        var requested = relayService.requestManualReviewRequeue("outbox-001", "ops-requester", "broker recovered");

        var rejected = relayService.rejectManualReviewRequeueRequest(
                requested.requestId(),
                "ops-rejector",
                "원인 조치 미확인"
        );

        assertThat(rejected.status()).isEqualTo(OperationOutboxRequeueRequestStatus.REJECTED);
        assertThat(rejected.rejectedBy()).isEqualTo("ops-rejector");
        assertThat(rejected.rejectionReason()).isEqualTo("원인 조치 미확인");
        assertThat(relayService.getRequeueAudits("outbox-001")).isEmpty();
        assertThat(repository.findOperationOutboxEvents("op-001"))
                .singleElement()
                .satisfies(outboxEvent -> assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.MANUAL_REVIEW));
    }

    @Test
    void rejectsSameRequesterAndRejector() {
        commandService.charge("member-001", "wallet-001", new WalletChargeCommand(money("5000"), "charge-001", "테스트 충전"));
        relayService.markFailed("outbox-001", "broker unavailable");
        relayService.markFailed("outbox-001", "broker unavailable");
        relayService.markFailed("outbox-001", "broker unavailable");
        var requested = relayService.requestManualReviewRequeue("outbox-001", "ops-user", "broker recovered");

        assertThatThrownBy(() -> relayService.rejectManualReviewRequeueRequest(
                requested.requestId(),
                "ops-user",
                "self rejection"
        ))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("rejector must be different from requester");
    }

    @Test
    void rejectsInvalidRelayInputs() {
        assertThatThrownBy(() -> relayService.getPendingEvents(0))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("limit must be positive");
        assertThatThrownBy(() -> relayService.getManualReviewEvents(0))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("limit must be positive");
        assertThatThrownBy(() -> relayService.claimReadyEvents(0))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("limit must be positive");
        assertThatThrownBy(() -> relayService.publishReadyEvents(0))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("limit must be positive");
        assertThatThrownBy(() -> relayService.markPublished(" "))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("outboxEventId must not be blank");
        assertThatThrownBy(() -> relayService.markFailed("outbox-001", " "))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("lastError must not be blank");
        assertThatThrownBy(() -> relayService.requeueManualReviewEvent(" ", "ops-user", "reason"))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("outboxEventId must not be blank");
        assertThatThrownBy(() -> relayService.requeueManualReviewEvent("outbox-001", " ", "reason"))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("operator must not be blank");
        assertThatThrownBy(() -> relayService.requeueManualReviewEvent("outbox-001", "ops-user", " "))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("reason must not be blank");
        assertThatThrownBy(() -> relayService.requestManualReviewRequeue(" ", "ops-user", "reason"))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("outboxEventId must not be blank");
        assertThatThrownBy(() -> relayService.approveManualReviewRequeueRequest(" ", "ops-user", "reason"))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("requestId must not be blank");
        assertThatThrownBy(() -> relayService.executeManualReviewRequeueRequest(" ", "ops-user"))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("requestId must not be blank");
        assertThatThrownBy(() -> relayService.rejectManualReviewRequeueRequest(" ", "ops-user", "reason"))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("requestId must not be blank");
    }

    private Money money(String amount) {
        return new Money(new BigDecimal(amount), "KRW");
    }
}
