package com.vetsoftware.app.state.infrastructure.persistence;

import com.vetsoftware.app.city.infrastructure.persistence.CityJpaRepository;
import com.vetsoftware.app.state.application.port.out.CityChildrenQueryPort;
import org.springframework.stereotype.Component;

@Component
public class JpaCityChildrenQueryPort implements CityChildrenQueryPort {
    private final CityJpaRepository jpaRepository;

    public JpaCityChildrenQueryPort(CityJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByStateId(Long parentId) {
        return jpaRepository.existsByState_Id(parentId);
    }
}
