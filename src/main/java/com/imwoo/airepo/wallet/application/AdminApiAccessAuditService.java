package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.AdminApiAccessAudit;
import com.imwoo.airepo.wallet.domain.AdminApiAccessOutcome;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AdminApiAccessAuditService {

    private final AdminApiAccessAuditRepository adminApiAccessAuditRepository;
    private final Clock clock;

    public AdminApiAccessAuditService(AdminApiAccessAuditRepository adminApiAccessAuditRepository, Clock clock) {
        this.adminApiAccessAuditRepository = adminApiAccessAuditRepository;
        this.clock = clock;
    }

    public void recordAccess(String method, String path, String operatorId, int statusCode) {
        adminApiAccessAuditRepository.saveAdminApiAccessAudit(new AdminApiAccessAudit(
                adminApiAccessAuditRepository.nextAdminApiAccessAuditId(),
                Instant.now(clock),
                required("method", method),
                required("path", path),
                normalizedOperatorId(operatorId),
                statusCode,
                outcome(statusCode)
        ));
    }

    public List<AdminApiAccessAudit> getRecentAudits(int limit) {
        if (limit <= 0) {
            throw new InvalidWalletOperationException("limit must be positive");
        }
        return adminApiAccessAuditRepository.findRecentAdminApiAccessAudits(limit);
    }

    private String required(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidWalletOperationException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private String normalizedOperatorId(String operatorId) {
        if (operatorId == null || operatorId.isBlank()) {
            return null;
        }
        return operatorId.trim();
    }

    private AdminApiAccessOutcome outcome(int statusCode) {
        if (statusCode < 400) {
            return AdminApiAccessOutcome.SUCCESS;
        }
        return AdminApiAccessOutcome.FAILURE;
    }
}
