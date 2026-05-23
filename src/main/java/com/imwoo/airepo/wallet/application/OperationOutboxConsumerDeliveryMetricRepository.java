package com.imwoo.airepo.wallet.application;

import java.time.Instant;

public interface OperationOutboxConsumerDeliveryMetricRepository {

    void recordConsumerDeliveryMetric(Instant occurredAt, boolean duplicate);
}
