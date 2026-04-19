package com.vetsoftware.app.membership.infrastructure.persistence;

import com.vetsoftware.app.membership.domain.Membership;
import org.springframework.stereotype.Component;

@Component
public class MembershipJpaMapper {
    public MembershipJpaEntity toJpa(Membership membership) {
        MembershipJpaEntity entity = new MembershipJpaEntity();
        entity.setId(membership.getId());
        entity.setName(membership.getName());
        entity.setStatus(membership.getStatus());
        entity.setCreatedDate(membership.getCreatedDate());
        entity.setCreatedBy(membership.getCreatedBy());
        return entity;
    }

    public Membership toDomain(MembershipJpaEntity entity) {
        return new Membership(
            entity.getId(),
            entity.getName(),
            entity.getStatus(),
            entity.getCreatedDate(),
            entity.getCreatedBy()
        );
    }
}
