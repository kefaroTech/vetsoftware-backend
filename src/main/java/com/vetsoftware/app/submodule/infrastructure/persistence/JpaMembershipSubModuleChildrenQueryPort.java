package com.vetsoftware.app.submodule.infrastructure.persistence;

import com.vetsoftware.app.membershipsubmodule.infrastructure.persistence.MembershipSubModuleJpaRepository;
import com.vetsoftware.app.submodule.application.port.out.MembershipSubModuleChildrenQueryPort;
import org.springframework.stereotype.Component;

@Component
public class JpaMembershipSubModuleChildrenQueryPort
        implements
            MembershipSubModuleChildrenQueryPort {
    private final MembershipSubModuleJpaRepository jpaRepository;

    public JpaMembershipSubModuleChildrenQueryPort(MembershipSubModuleJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveBySubModuleId(Long parentId) {
        return jpaRepository.existsBySubModule_Id(parentId);
    }
}
