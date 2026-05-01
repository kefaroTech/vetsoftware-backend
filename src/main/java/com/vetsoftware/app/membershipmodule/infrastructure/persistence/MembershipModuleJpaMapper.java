package com.vetsoftware.app.membershipmodule.infrastructure.persistence;

import com.vetsoftware.app.membership.infrastructure.persistence.MembershipJpaEntity;
import com.vetsoftware.app.membershipmodule.domain.MembershipModule;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class MembershipModuleJpaMapper {

    public MembershipModuleJpaEntity toJpa(MembershipModule membershipModule,
                                            MembershipJpaEntity membership,
                                            SubModuleJpaEntity subModule) {
        MembershipModuleJpaEntity entity = new MembershipModuleJpaEntity();
        entity.setId(membershipModule.getId());
        entity.setMembership(membership);
        entity.setSubModule(subModule);
        entity.setCreatedDate(membershipModule.getCreatedDate());
        return entity;
    }

    public MembershipModule toDomain(MembershipModuleJpaEntity entity) {
        return new MembershipModule(
            entity.getId(),
            entity.getMembership().getId(),
            entity.getSubModule().getId(),
            entity.getCreatedDate()
        );
    }
}
