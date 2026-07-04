package com.imwoo.airepo.wallet.infra;

import com.imwoo.airepo.wallet.application.AdminApiAccessAuditRepository;
import com.imwoo.airepo.wallet.domain.AdminApiAccessAudit;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * JDBC adapter for the admin API access audit bounded context: audit id issuance, persistence,
 * recent history, and retention pruning.
 */
final class JdbcAdminApiAccessAuditRepository implements AdminApiAccessAuditRepository {

    private final WalletJdbcSupport support;
    private final JdbcTemplate jdbcTemplate;

    JdbcAdminApiAccessAuditRepository(WalletJdbcSupport support) {
        this.support = support;
        this.jdbcTemplate = support.jdbc();
    }

    @Override
    public String nextAdminApiAccessAuditId() {
        return support.nextId("admin-api-access-audit", "admin_api_access_audit_id_seq");
    }

    @Override
    public void saveAdminApiAccessAudit(AdminApiAccessAudit accessAudit) {
        jdbcTemplate.update(
                """
                        insert into admin_api_access_audits (
                            audit_id, occurred_at, method, path, operator_id, status_code, outcome
                        )
                        values (?, ?, ?, ?, ?, ?, ?)
                        """,
                accessAudit.auditId(),
                support.timestamp(accessAudit.occurredAt()),
                accessAudit.method(),
                accessAudit.path(),
                accessAudit.operatorId(),
                accessAudit.statusCode(),
                accessAudit.outcome().name()
        );
    }

    @Override
    public List<AdminApiAccessAudit> findRecentAdminApiAccessAudits(int limit) {
        return jdbcTemplate.query(
                """
                        select audit_id, occurred_at, method, path, operator_id, status_code, outcome
                        from admin_api_access_audits
                        order by occurred_at desc, audit_id desc
                        limit ?
                        """,
                support.adminApiAccessAuditMapper(),
                limit
        );
    }

    @Override
    public int deleteAdminApiAccessAuditsOccurredBefore(Instant cutoff) {
        return jdbcTemplate.update(
                "delete from admin_api_access_audits where occurred_at < ?",
                support.timestamp(cutoff)
        );
    }
}
