package com.imwoo.airepo.wallet.application;

public class MemberNotFoundException extends RuntimeException {

    public MemberNotFoundException(String memberId) {
        super("Member not found: " + memberId);
    }
}
