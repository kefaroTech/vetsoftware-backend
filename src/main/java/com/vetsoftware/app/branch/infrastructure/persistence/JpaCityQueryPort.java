package com.vetsoftware.app.branch.infrastructure.persistence;

import com.vetsoftware.app.branch.application.port.out.CityQueryPort;
import com.vetsoftware.app.branch.domain.CityRef;
import com.vetsoftware.app.city.infrastructure.persistence.CityJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("branchJpaCityQueryPort")
public class JpaCityQueryPort implements CityQueryPort {
    private final CityJpaRepository cityJpaRepository;

    public JpaCityQueryPort(CityJpaRepository cityJpaRepository) {
        this.cityJpaRepository = cityJpaRepository;
    }

    @Override
    public Optional<CityRef> findById(Long cityId) {
        return cityJpaRepository.findById(cityId)
            .map(e -> new CityRef(e.getId(), e.getName()));
    }
}
