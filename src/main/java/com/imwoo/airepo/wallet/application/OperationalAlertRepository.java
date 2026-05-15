package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.OperationalAlert;
import java.util.List;

public interface OperationalAlertRepository {

    String nextOperationalAlertId();

    void saveOperationalAlert(OperationalAlert operationalAlert);

    List<OperationalAlert> findRecentOperationalAlerts(int limit);
}
