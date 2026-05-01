package com.vetsoftware.app.membershipsubmodule.infrastructure.persistence;

import com.vetsoftware.app.membership.infrastructure.persistence.MembershipJpaEntity;
import com.vetsoftware.app.membershipsubmodule.domain.MembershipSubModule;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class MembershipSubModuleJpaMapper {

    public MembershipSubModuleJpaEntity toJpa(MembershipSubModule membershipSubModule,
                                               MembershipJpaEntity membership,
                                               SubModuleJpaEntity subModule) {
        MembershipSubModuleJpaEntity entity = new MembershipSubModuleJpaEntity();
        entity.setId(membershipSubModule.getId());
        entity.setMembership(membership);
        entity.setSubModule(subModule);
        entity.setCreatedDate(membershipSubModule.getCreatedDate());
        return entity;
    }

    public MembershipSubModule toDomain(MembershipSubModuleJpaEntity entity) {
        return new MembershipSubModule(
            entity.getId(),
            entity.getMembership().getId(),
            entity.getSubModule().getId(),
            entity.getCreatedDate()
        );
    }
}
