package com.imwoo.airepo.wallet.infra;

import com.imwoo.airepo.wallet.application.OperationalAlertPublisher;
import com.imwoo.airepo.wallet.domain.OperationalAlert;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "ai-repo.operational-alert.publisher",
        name = "type",
        havingValue = "none",
        matchIfMissing = true
)
public class NoopOperationalAlertPublisher implements OperationalAlertPublisher {

    @Override
    public void publish(OperationalAlert operationalAlert) {
        // Intentionally empty: the default local profile records alerts without sending external notifications.
    }
}
