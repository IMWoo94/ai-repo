package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.OperationalAlert;

public interface OperationalAlertPublisher {

    void publish(OperationalAlert operationalAlert);
}
