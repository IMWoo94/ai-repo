package com.imwoo.airepo.wallet.application;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OperationalAlertPolicy {

    private final Duration suppressionWindow;
    private final Duration retention;

    public OperationalAlertPolicy(
            @Value("${ai-repo.operational-alert.suppression-window-minutes:15}") int suppressionWindowMinutes,
            @Value("${ai-repo.operational-alert.retention-days:30}") int retentionDays
    ) {
        this.suppressionWindow = minutes("suppression window", suppressionWindowMinutes);
        this.retention = days("retention", retentionDays);
    }

    public Duration suppressionWindow() {
        return suppressionWindow;
    }

    public Duration retention() {
        return retention;
    }

    private Duration minutes(String name, int minutes) {
        if (minutes <= 0) {
            throw new IllegalArgumentException(name + " minutes must be positive");
        }
        return Duration.ofMinutes(minutes);
    }

    private Duration days(String name, int days) {
        if (days <= 0) {
            throw new IllegalArgumentException(name + " days must be positive");
        }
        return Duration.ofDays(days);
    }
}
