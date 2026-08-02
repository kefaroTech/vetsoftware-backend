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
        entity.setMandatory(membership.isMandatory());
        entity.setCreatedDate(membership.getCreatedDate());
        entity.setEnabled(membership.isEnabled());
        return entity;
    }

    public Membership toDomain(MembershipJpaEntity entity) {
        return new Membership(entity.getId(), entity.getName(),
                MembershipStatus.valueOf(entity.getStatus()),
                Boolean.TRUE.equals(entity.getMandatory()), entity.getCreatedDate(),
                entity.isEnabled());
    }
}
