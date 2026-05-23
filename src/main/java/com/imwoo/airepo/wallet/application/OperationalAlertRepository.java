package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.OperationalAlert;
import com.imwoo.airepo.wallet.domain.OperationalAlertSeverity;
import java.time.Instant;
import java.util.List;

public interface OperationalAlertRepository {

    String nextOperationalAlertId();

    void saveOperationalAlert(OperationalAlert operationalAlert);

    boolean existsOperationalAlertBetween(
            String source,
            OperationalAlertSeverity severity,
            List<String> reasons,
            Instant since,
            Instant until
    );

    List<OperationalAlert> findRecentOperationalAlerts(int limit);

    int deleteOperationalAlertsOccurredBefore(Instant cutoff);
}
