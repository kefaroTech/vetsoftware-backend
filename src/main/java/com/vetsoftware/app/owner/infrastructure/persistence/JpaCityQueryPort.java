package com.vetsoftware.app.owner.infrastructure.persistence;

import com.vetsoftware.app.city.infrastructure.persistence.CityJpaRepository;
import com.vetsoftware.app.owner.application.port.out.CityQueryPort;
import com.vetsoftware.app.owner.domain.CityRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("ownerJpaCityQueryPort")
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
