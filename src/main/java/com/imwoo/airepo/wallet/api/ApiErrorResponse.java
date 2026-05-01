package com.imwoo.airepo.wallet.api;

import java.time.Instant;

public record ApiErrorResponse(String code, String message, Instant timestamp) {
}
