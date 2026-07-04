package com.imwoo.airepo.wallet.application;

public class MemberNotActiveException extends RuntimeException {

    public MemberNotActiveException(String memberId) {
        super("Member is not active: " + memberId);
    }
}
