package com.imwoo.airepo.wallet.config;

import com.imwoo.airepo.wallet.application.MemberLookup;
import com.imwoo.airepo.wallet.application.WalletQueryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthBeansConfig {

    @Bean
    MemberLookup memberLookup(WalletQueryRepository walletQueryRepository) {
        return walletQueryRepository::findMember;
    }
}
