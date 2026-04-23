package com.vetsoftware.app.membership.infrastructure.persistence;

import com.vetsoftware.app.membership.domain.Membership;
import com.vetsoftware.app.membership.domain.MembershipStatus;
import com.vetsoftware.app.module.infrastructure.persistence.ModuleJpaEntity;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MembershipJpaMapper {

    public MembershipJpaEntity toJpa(Membership membership, List<ModuleJpaEntity> modules) {
        MembershipJpaEntity entity = new MembershipJpaEntity();
        entity.setId(membership.getId());
        entity.setName(membership.getName());
        entity.setStatus(membership.getStatus().name());
        entity.setCreatedDate(membership.getCreatedDate());
        entity.setModules(modules);
        return entity;
    }

    public Membership toDomain(MembershipJpaEntity entity) {
        List<Long> moduleIds = entity.getModules().stream()
            .map(ModuleJpaEntity::getId)
            .toList();
        return new Membership(
            entity.getId(),
            entity.getName(),
            MembershipStatus.valueOf(entity.getStatus()),
            entity.getCreatedDate(),
            moduleIds
        );
    }
}
