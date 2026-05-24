package com.imwoo.airepo.wallet.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.imwoo.airepo.wallet.application.AdminApiAccessAuditService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AdminApiAccessAuditFilterTest {

    private final AdminApiAccessAuditService auditService = org.mockito.Mockito.mock(AdminApiAccessAuditService.class);
    private final AdminApiAccessAuditFilter filter = new AdminApiAccessAuditFilter(auditService);

    @Test
    void recordsSegmentMatchedAdminApiAccess() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/outbox-events/outbox-001/requeue-audits");
        request.addHeader(AdminAuthorizationGuard.OPERATOR_ID_HEADER, " ops-user ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            ((MockHttpServletResponse) servletResponse).setStatus(403);
        });

        verify(auditService).recordAccess("GET", "/api/v1/outbox-events/outbox-001/requeue-audits", " ops-user ", 403);
    }

    @Test
    void doesNotRecordLookalikePublicPrefix() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/outbox-events-v2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            ((MockHttpServletResponse) servletResponse).setStatus(200);
        });

        verifyNoInteractions(auditService);
    }
}
