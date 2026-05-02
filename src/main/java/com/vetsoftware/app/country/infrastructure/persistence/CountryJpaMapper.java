package com.vetsoftware.app.country.infrastructure.persistence;

import com.vetsoftware.app.country.domain.Country;
import org.springframework.stereotype.Component;

@Component
public class CountryJpaMapper {
    public CountryJpaEntity toJpa(Country country) {
        CountryJpaEntity entity = new CountryJpaEntity();
        entity.setId(country.getId());
        entity.setName(country.getName());
        entity.setCreatedDate(country.getCreatedDate());
        return entity;
    }

    public Country toDomain(CountryJpaEntity entity) {
        return new Country(entity.getId(), entity.getName(), entity.getCreatedDate());
    }
}
