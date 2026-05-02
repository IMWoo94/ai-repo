package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.OperationOutboxRelayRun;
import java.util.List;

public interface OperationOutboxRelayRunRepository {

    String nextRelayRunId();

    void saveOutboxRelayRun(OperationOutboxRelayRun relayRun);

    List<OperationOutboxRelayRun> findRecentOutboxRelayRuns(int limit);
}
