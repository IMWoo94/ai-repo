package com.imwoo.airepo.wallet.infra;

import com.imwoo.airepo.wallet.application.OperationalAlertRepository;
import com.imwoo.airepo.wallet.domain.OperationalAlert;
import com.imwoo.airepo.wallet.domain.OperationalAlertSeverity;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * JDBC adapter for the operational alert bounded context: alert id issuance, persistence,
 * suppression-window lookup, recent history, and retention pruning.
 */
final class JdbcOperationalAlertRepository implements OperationalAlertRepository {

    private final WalletJdbcSupport support;
    private final JdbcTemplate jdbcTemplate;

    JdbcOperationalAlertRepository(WalletJdbcSupport support) {
        this.support = support;
        this.jdbcTemplate = support.jdbc();
    }

    @Override
    public String nextOperationalAlertId() {
        return support.nextId("operational-alert", "operational_alert_id_seq");
    }

    @Override
    public void saveOperationalAlert(OperationalAlert operationalAlert) {
        jdbcTemplate.update(
                """
                        insert into operational_alerts (
                            alert_id, source, severity, occurred_at, reasons
                        )
                        values (?, ?, ?, ?, ?)
                        """,
                operationalAlert.alertId(),
                operationalAlert.source(),
                operationalAlert.severity().name(),
                support.timestamp(operationalAlert.occurredAt()),
                String.join("\n", operationalAlert.reasons())
        );
    }

    @Override
    public boolean existsOperationalAlertBetween(
            String source,
            OperationalAlertSeverity severity,
            List<String> reasons,
            Instant since,
            Instant until
    ) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from operational_alerts
                        where source = ?
                          and severity = ?
                          and reasons = ?
                          and occurred_at >= ?
                          and occurred_at <= ?
                        """,
                Integer.class,
                source,
                severity.name(),
                String.join("\n", reasons),
                support.timestamp(since),
                support.timestamp(until)
        );
        return count != null && count > 0;
    }

    @Override
    public List<OperationalAlert> findRecentOperationalAlerts(int limit) {
        return jdbcTemplate.query(
                """
                        select alert_id, source, severity, occurred_at, reasons
                        from operational_alerts
                        order by occurred_at desc, alert_id desc
                        limit ?
                        """,
                support.operationalAlertMapper(),
                limit
        );
    }

    @Override
    public int deleteOperationalAlertsOccurredBefore(Instant cutoff) {
        return jdbcTemplate.update(
                "delete from operational_alerts where occurred_at < ?",
                support.timestamp(cutoff)
        );
    }
}
