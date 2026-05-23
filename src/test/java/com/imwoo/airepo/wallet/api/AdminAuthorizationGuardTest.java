package com.imwoo.airepo.wallet.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AdminAuthorizationGuardTest {

    private final AdminAuthorizationGuard guard = new AdminAuthorizationGuard(
            new AdminAuthorizationProperties("local-ops-token", "local-operator-token")
    );

    @Test
    void authenticatesValidAdminTokenAndTrimsOperatorId() {
        AdminOperator operator = guard.authenticate("local-ops-token", " ops-user ");

        assertThat(operator.operatorId()).isEqualTo("ops-user");
    }

    @Test
    void rejectsMissingAdminTokenBeforeOperatorValidation() {
        assertThatThrownBy(() -> guard.authenticate(" ", "ops-user"))
                .isInstanceOf(AdminAuthenticationException.class)
                .hasMessage("admin token is required");
    }

    @Test
    void rejectsInvalidAdminToken() {
        assertThatThrownBy(() -> guard.authenticate("wrong-token", "ops-user"))
                .isInstanceOf(AdminAuthenticationException.class)
                .hasMessage("admin token is invalid");
    }

    @Test
    void rejectsMissingOperatorIdAfterSuccessfulTokenValidation() {
        assertThatThrownBy(() -> guard.authenticate("local-ops-token", " "))
                .isInstanceOf(AdminAuthorizationException.class)
                .hasMessage("operator id is required");
    }
}
