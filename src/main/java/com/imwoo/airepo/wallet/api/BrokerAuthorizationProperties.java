package com.imwoo.airepo.wallet.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BrokerAuthorizationProperties {

    private final String brokerToken;

    public BrokerAuthorizationProperties(
            @Value("${ai-repo.outbox.consumer.broker-token:local-broker-token}") String brokerToken
    ) {
        if (brokerToken == null || brokerToken.isBlank()) {
            throw new IllegalArgumentException("ai-repo.outbox.consumer.broker-token must not be blank");
        }
        this.brokerToken = brokerToken;
    }

    public String brokerToken() {
        return brokerToken;
    }
}
