package com.imwoo.airepo.wallet.application;

import java.time.Instant;

public interface OperationOutboxConsumerPruningRepository {

    int deleteConsumerProcessedEventsProcessedBefore(Instant cutoff);

    int deleteConsumerReceiptsReceivedBefore(Instant cutoff);

    int deleteConsumerDeliveryMetricsBucketStartedBefore(Instant cutoff);
}
