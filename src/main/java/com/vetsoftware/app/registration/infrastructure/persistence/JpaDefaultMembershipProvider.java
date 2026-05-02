package com.vetsoftware.app.registration.infrastructure.persistence;

import com.vetsoftware.app.membership.infrastructure.persistence.MembershipJpaEntity;
import com.vetsoftware.app.membership.infrastructure.persistence.MembershipJpaRepository;
import com.vetsoftware.app.registration.application.port.out.DefaultMembershipProvider;
import org.springframework.stereotype.Component;

@Component
public class JpaDefaultMembershipProvider implements DefaultMembershipProvider {

    private static final String DEFAULT_NAME = "FULL";
    private static final String DEFAULT_STATUS = "ACTIVE";

    private final MembershipJpaRepository membershipJpaRepository;

    public JpaDefaultMembershipProvider(MembershipJpaRepository membershipJpaRepository) {
        this.membershipJpaRepository = membershipJpaRepository;
    }

    @Override
    public Long getDefaultMembershipId() {
        return membershipJpaRepository
            .findFirstByNameAndStatus(DEFAULT_NAME, DEFAULT_STATUS)
            .map(MembershipJpaEntity::getId)
            .orElseThrow(() -> new IllegalStateException(
                "Default membership not found: name=" + DEFAULT_NAME + ", status=" + DEFAULT_STATUS));
    }
}
