package com.vetsoftware.app.registration.infrastructure.persistence;

import com.vetsoftware.app.membership.infrastructure.persistence.MembershipJpaEntity;
import com.vetsoftware.app.membership.infrastructure.persistence.MembershipJpaRepository;
import com.vetsoftware.app.registration.application.port.out.DefaultMembershipProvider;
import org.springframework.stereotype.Component;

@Component
public class JpaDefaultMembershipProvider implements DefaultMembershipProvider {

    private final MembershipJpaRepository membershipJpaRepository;

    public JpaDefaultMembershipProvider(MembershipJpaRepository membershipJpaRepository) {
        this.membershipJpaRepository = membershipJpaRepository;
    }

    @Override
    public Long getDefaultMembershipId() {
        return membershipJpaRepository
            .findFirstByMandatoryTrue()
            .map(MembershipJpaEntity::getId)
            .orElseThrow(() -> new IllegalStateException(
                "No mandatory membership found. Mark one membership with mandatory=true."));
    }
}
