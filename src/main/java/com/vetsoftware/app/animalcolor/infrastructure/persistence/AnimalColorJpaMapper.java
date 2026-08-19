package com.vetsoftware.app.animalcolor.infrastructure.persistence;

import com.vetsoftware.app.animalcolor.domain.AnimalColor;
import com.vetsoftware.app.animalcolor.domain.SpecieRef;
import com.vetsoftware.app.specie.infrastructure.persistence.SpecieJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class AnimalColorJpaMapper {

    public AnimalColorJpaEntity toJpa(AnimalColor color, SpecieJpaEntity specie) {
        AnimalColorJpaEntity entity = new AnimalColorJpaEntity();
        entity.setId(color.getId());
        entity.setName(color.getName());
        entity.setSpecie(specie);
        entity.setCreatedDate(color.getCreatedDate());
        entity.setVersion(color.getVersion());
        entity.setEnabled(color.isEnabled());
        return entity;
    }

    public AnimalColor toDomain(AnimalColorJpaEntity entity) {
        SpecieJpaEntity s = entity.getSpecie();
        return toDomain(entity, new SpecieRef(s.getId(), s.getName()));
    }

    public AnimalColor toDomain(AnimalColorJpaEntity entity, SpecieRef ref) {
        return new AnimalColor(entity.getId(), entity.getName(), ref, entity.getCreatedDate(),
                entity.getVersion(), entity.isEnabled());
    }
}
