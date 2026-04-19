package com.vetsoftware.app.membership.infrastructure.persistence;

import com.vetsoftware.app.membership.domain.Membership;
import com.vetsoftware.app.membership.domain.MembershipStatus;
import org.springframework.stereotype.Component;

@Component
public class MembershipJpaMapper {
    public MembershipJpaEntity toJpa(Membership membership) {
        MembershipJpaEntity entity = new MembershipJpaEntity();
        entity.setId(membership.getId());
        entity.setName(membership.getName());
        entity.setStatus(membership.getStatus().name());
        entity.setCreatedDate(membership.getCreatedDate());
        return entity;
    }

    public Membership toDomain(MembershipJpaEntity entity) {
        return new Membership(
            entity.getId(),
            entity.getName(),
            MembershipStatus.valueOf(entity.getStatus()),
            entity.getCreatedDate()
        );
    }
}
