package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.OperationOutboxConsumerReceipt;
import java.util.Optional;

public interface OperationOutboxConsumerReceiptRepository {

    void saveConsumerReceipt(OperationOutboxConsumerReceipt receipt);

    Optional<OperationOutboxConsumerReceipt> findConsumerReceipt(String idempotencyKey);
}
