package com.vetsoftware.app.membership.infrastructure.persistence;

import com.vetsoftware.app.membership.application.port.out.MembershipSubModuleChildrenQueryPort;
import com.vetsoftware.app.membershipsubmodule.infrastructure.persistence.MembershipSubModuleJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaMembershipSubModuleChildrenQueryPort implements MembershipSubModuleChildrenQueryPort {
    private final MembershipSubModuleJpaRepository jpaRepository;

    public JpaMembershipSubModuleChildrenQueryPort(MembershipSubModuleJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByMembershipId(Long parentId) {
        return jpaRepository.existsByMembership_Id(parentId);
    }
}
