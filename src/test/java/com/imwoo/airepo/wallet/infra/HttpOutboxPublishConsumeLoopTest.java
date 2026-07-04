package com.imwoo.airepo.wallet.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.imwoo.airepo.AiRepoApplication;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerReceiptRepository;
import com.imwoo.airepo.wallet.application.OperationOutboxPublishBatchResult;
import com.imwoo.airepo.wallet.application.OperationOutboxRelayRepository;
import com.imwoo.airepo.wallet.application.OperationOutboxRelayService;
import com.imwoo.airepo.wallet.application.WalletChargeCommand;
import com.imwoo.airepo.wallet.application.WalletCommandService;
import com.imwoo.airepo.wallet.application.WalletLedgerQueryService;
import com.imwoo.airepo.wallet.domain.Money;
import com.imwoo.airepo.wallet.domain.OperationOutboxStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(
        classes = {AiRepoApplication.class, HttpOutboxPublishConsumeLoopTest.FixedClockConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class HttpOutboxPublishConsumeLoopTest {

    private final int port;
    private final WalletCommandService walletCommandService;
    private final WalletLedgerQueryService walletLedgerQueryService;
    private final OperationOutboxRelayRepository relayRepository;
    private final OperationOutboxConsumerReceiptRepository receiptRepository;
    private final Clock clock;

    @Autowired
    HttpOutboxPublishConsumeLoopTest(
            @LocalServerPort int port,
            WalletCommandService walletCommandService,
            WalletLedgerQueryService walletLedgerQueryService,
            OperationOutboxRelayRepository relayRepository,
            OperationOutboxConsumerReceiptRepository receiptRepository,
            Clock clock
    ) {
        this.port = port;
        this.walletCommandService = walletCommandService;
        this.walletLedgerQueryService = walletLedgerQueryService;
        this.relayRepository = relayRepository;
        this.receiptRepository = receiptRepository;
        this.clock = clock;
    }

    @Test
    void relayPublishesThroughHttpConsumerAndLeavesOneReceipt() {
        walletCommandService.charge(
                "member-001",
                "wallet-001",
                new WalletChargeCommand(
                        new Money(new BigDecimal("5000"), "KRW"),
                        "publish-consume-loop-charge-001",
                        "publish consume loop charge"
                )
        );
        OperationOutboxRelayService relayService = new OperationOutboxRelayService(
                clock,
                relayRepository,
                new HttpOperationOutboxPublisher(
                        "http://127.0.0.1:%d/internal/broker/outbox-events".formatted(port),
                        3000,
                        "local-broker-token"
                )
        );

        OperationOutboxPublishBatchResult result = relayService.publishReadyEvents(10);
        OperationOutboxPublishBatchResult duplicateResult = relayService.publishReadyEvents(10);

        assertThat(result.claimedCount()).isEqualTo(1);
        assertThat(result.publishedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isZero();
        assertThat(duplicateResult.claimedCount()).isZero();
        assertThat(receiptRepository.findConsumerReceipt("outbox-001"))
                .hasValueSatisfying(receipt -> {
                    assertThat(receipt.outboxEventId()).isEqualTo("outbox-001");
                    assertThat(receipt.operationId()).isEqualTo("op-001");
                    assertThat(receipt.eventType()).isEqualTo("CHARGE_COMPLETED");
                    assertThat(receipt.receivedAt()).isEqualTo(Instant.parse("2026-05-01T00:00:00Z"));
                });
        assertThat(relayRepository.findPendingOutboxEvents(10)).isEmpty();
        assertThat(relayRepository.findManualReviewOutboxEvents(10)).isEmpty();
        assertThat(relayRepository.claimReadyOutboxEvents(
                10,
                Instant.parse("2026-05-01T00:00:00Z"),
                Instant.parse("2026-05-01T00:01:00Z")
        )).isEmpty();
        assertThat(walletLedgerQueryService.getOperationOutboxEvents("op-001"))
                .singleElement()
                .satisfies(outboxEvent -> assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.PUBLISHED));
    }

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC);
        }
    }
}
