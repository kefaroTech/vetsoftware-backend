package com.vetsoftware.app.country.infrastructure.persistence;

import com.vetsoftware.app.country.application.port.out.StateChildrenQueryPort;
import com.vetsoftware.app.state.infrastructure.persistence.StateJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaStateChildrenQueryPort implements StateChildrenQueryPort {
    private final StateJpaRepository jpaRepository;

    public JpaStateChildrenQueryPort(StateJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByCountryId(Long parentId) {
        return jpaRepository.existsByCountry_Id(parentId);
    }
}
