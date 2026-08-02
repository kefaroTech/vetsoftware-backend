package com.vetsoftware.app.animalcolor.infrastructure.persistence;

import com.vetsoftware.app.animalcolor.domain.AnimalColor;
import org.springframework.stereotype.Component;

@Component
public class AnimalColorJpaMapper {
  public AnimalColorJpaEntity toJpa(AnimalColor color) {
    AnimalColorJpaEntity entity = new AnimalColorJpaEntity();
    entity.setId(color.getId());
    entity.setName(color.getName());
    entity.setCreatedDate(color.getCreatedDate());
    entity.setEnabled(color.isEnabled());
    return entity;
  }

  public AnimalColor toDomain(AnimalColorJpaEntity entity) {
    return new AnimalColor(
        entity.getId(), entity.getName(), entity.getCreatedDate(), entity.isEnabled());
  }
}
