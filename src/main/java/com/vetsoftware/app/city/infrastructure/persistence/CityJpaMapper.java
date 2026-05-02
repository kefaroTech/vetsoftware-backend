package com.vetsoftware.app.city.infrastructure.persistence;

import com.vetsoftware.app.city.domain.City;
import com.vetsoftware.app.city.domain.StateRef;
import com.vetsoftware.app.state.infrastructure.persistence.StateJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class CityJpaMapper {

    public CityJpaEntity toJpa(City city, StateJpaEntity state) {
        CityJpaEntity entity = new CityJpaEntity();
        entity.setId(city.getId());
        entity.setName(city.getName());
        entity.setState(state);
        entity.setCreatedDate(city.getCreatedDate());
        return entity;
    }

    public City toDomain(CityJpaEntity entity) {
        StateJpaEntity s = entity.getState();
        return toDomain(entity, new StateRef(s.getId(), s.getName()));
    }

    public City toDomain(CityJpaEntity entity, StateRef ref) {
        return new City(entity.getId(), entity.getName(), ref, entity.getCreatedDate());
    }
}
