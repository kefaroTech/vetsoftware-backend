package com.vetsoftware.app.state.infrastructure.persistence;

import com.vetsoftware.app.country.infrastructure.persistence.CountryJpaEntity;
import com.vetsoftware.app.state.domain.CountryRef;
import com.vetsoftware.app.state.domain.State;
import org.springframework.stereotype.Component;

@Component
public class StateJpaMapper {

    public StateJpaEntity toJpa(State state, CountryJpaEntity country) {
        StateJpaEntity entity = new StateJpaEntity();
        entity.setId(state.getId());
        entity.setName(state.getName());
        entity.setCountry(country);
        entity.setCreatedDate(state.getCreatedDate());
        return entity;
    }

    public State toDomain(StateJpaEntity entity) {
        CountryJpaEntity c = entity.getCountry();
        return toDomain(entity, new CountryRef(c.getId(), c.getName()));
    }

    public State toDomain(StateJpaEntity entity, CountryRef ref) {
        return new State(entity.getId(), entity.getName(), ref, entity.getCreatedDate());
    }
}
