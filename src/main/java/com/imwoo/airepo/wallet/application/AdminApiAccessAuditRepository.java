package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.AdminApiAccessAudit;
import java.time.Instant;
import java.util.List;

public interface AdminApiAccessAuditRepository {

    String nextAdminApiAccessAuditId();

    void saveAdminApiAccessAudit(AdminApiAccessAudit accessAudit);

    List<AdminApiAccessAudit> findRecentAdminApiAccessAudits(int limit);

    int deleteAdminApiAccessAuditsOccurredBefore(Instant cutoff);
}
