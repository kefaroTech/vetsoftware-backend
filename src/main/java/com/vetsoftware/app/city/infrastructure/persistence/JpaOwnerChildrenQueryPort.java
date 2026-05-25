package com.vetsoftware.app.city.infrastructure.persistence;

import com.vetsoftware.app.city.application.port.out.OwnerChildrenQueryPort;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaOwnerChildrenQueryPort implements OwnerChildrenQueryPort {
    private final OwnerJpaRepository jpaRepository;

    public JpaOwnerChildrenQueryPort(OwnerJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByCityId(Long parentId) {
        return jpaRepository.existsByCity_Id(parentId);
    }
}
