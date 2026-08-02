package com.vetsoftware.app.specie.infrastructure.persistence;

import com.vetsoftware.app.specie.domain.Specie;
import org.springframework.stereotype.Component;

@Component
public class SpecieJpaMapper {
    public SpecieJpaEntity toJpa(Specie specie) {
        SpecieJpaEntity entity = new SpecieJpaEntity();
        entity.setId(specie.getId());
        entity.setName(specie.getName());
        entity.setCreatedDate(specie.getCreatedDate());
        entity.setEnabled(specie.isEnabled());
        return entity;
    }

    public Specie toDomain(SpecieJpaEntity entity) {
        return new Specie(entity.getId(), entity.getName(), entity.getCreatedDate(),
                entity.isEnabled());
    }
}
