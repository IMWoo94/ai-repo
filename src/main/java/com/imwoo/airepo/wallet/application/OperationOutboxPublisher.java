package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.OperationOutboxEvent;

public interface OperationOutboxPublisher {

    void publish(OperationOutboxEvent outboxEvent);
}
