package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.Member;
import java.util.Optional;

@FunctionalInterface
public interface MemberLookup {

    Optional<Member> findMember(String memberId);
}
