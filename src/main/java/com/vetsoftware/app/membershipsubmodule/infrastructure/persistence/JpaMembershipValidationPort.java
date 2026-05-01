package com.vetsoftware.app.membershipsubmodule.infrastructure.persistence;

import com.vetsoftware.app.membership.infrastructure.persistence.MembershipJpaRepository;
import com.vetsoftware.app.membershipsubmodule.application.port.out.MembershipValidationPort;
import org.springframework.stereotype.Component;

@Component("membershipSubModuleJpaMembershipValidationPort")
public class JpaMembershipValidationPort implements MembershipValidationPort {
    private final MembershipJpaRepository membershipJpaRepository;

    public JpaMembershipValidationPort(MembershipJpaRepository membershipJpaRepository) {
        this.membershipJpaRepository = membershipJpaRepository;
    }

    @Override
    public void validateExists(Long membershipId) {
        if (!membershipJpaRepository.existsById(membershipId)) {
            throw new IllegalArgumentException("Membership not found: " + membershipId);
        }
    }
}
