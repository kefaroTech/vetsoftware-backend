package com.vetsoftware.app.animalcolor.infrastructure.persistence;

import com.vetsoftware.app.animalcolor.application.port.out.AnimalColorRepository;
import com.vetsoftware.app.animalcolor.domain.AnimalColor;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAnimalColorRepository implements AnimalColorRepository {
  private final AnimalColorJpaRepository jpaRepository;
  private final AnimalColorJpaMapper mapper;

  public JpaAnimalColorRepository(
      AnimalColorJpaRepository jpaRepository, AnimalColorJpaMapper mapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }

  @Override
  public AnimalColor save(AnimalColor color) {
    return mapper.toDomain(jpaRepository.save(mapper.toJpa(color)));
  }

  @Override
  public Optional<AnimalColor> findById(Long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<AnimalColor> findAll() {
    return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
  }

  @Override
  public void delete(Long id) {
    jpaRepository.deleteById(id);
  }

  @Override
  public int reactivate(Long id) {
    return jpaRepository.reactivate(id);
  }
}
