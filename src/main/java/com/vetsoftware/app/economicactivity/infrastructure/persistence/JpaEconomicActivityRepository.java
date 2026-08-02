package com.vetsoftware.app.economicactivity.infrastructure.persistence;

import com.vetsoftware.app.economicactivity.application.port.out.EconomicActivityRepository;
import com.vetsoftware.app.economicactivity.domain.EconomicActivity;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaEconomicActivityRepository implements EconomicActivityRepository {
  private final EconomicActivityJpaRepository jpaRepository;
  private final EconomicActivityJpaMapper mapper;

  public JpaEconomicActivityRepository(
      EconomicActivityJpaRepository jpaRepository, EconomicActivityJpaMapper mapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }

  @Override
  public EconomicActivity save(EconomicActivity economicActivity) {
    return mapper.toDomain(jpaRepository.save(mapper.toJpa(economicActivity)));
  }

  @Override
  public Optional<EconomicActivity> findById(Long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<EconomicActivity> findAll() {
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

  @Override
  public boolean existsByCode(String code) {
    return jpaRepository.existsByCode(code);
  }
}
