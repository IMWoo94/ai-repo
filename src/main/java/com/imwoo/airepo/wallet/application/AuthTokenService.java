package com.imwoo.airepo.wallet.application;

public interface AuthTokenService {

    AuthToken issueToken(String memberId);
}
