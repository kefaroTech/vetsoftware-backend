package com.vetsoftware.app.state.infrastructure.persistence;

import com.vetsoftware.app.country.infrastructure.persistence.CountryJpaEntity;
import com.vetsoftware.app.country.infrastructure.persistence.CountryJpaRepository;
import com.vetsoftware.app.state.application.port.out.StateRepository;
import com.vetsoftware.app.state.domain.State;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaStateRepository implements StateRepository {
  private final StateJpaRepository jpaRepository;
  private final StateJpaMapper mapper;
  private final CountryJpaRepository countryJpaRepository;

  public JpaStateRepository(
      StateJpaRepository jpaRepository,
      StateJpaMapper mapper,
      CountryJpaRepository countryJpaRepository) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
    this.countryJpaRepository = countryJpaRepository;
  }

  @Override
  public State save(State state) {
    CountryJpaEntity country = countryJpaRepository.getReferenceById(state.getCountry().id());
    StateJpaEntity saved = jpaRepository.save(mapper.toJpa(state, country));
    return mapper.toDomain(saved, state.getCountry());
  }

  @Override
  public Optional<State> findById(Long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<State> findAll() {
    return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<State> findByCountryId(Long countryId) {
    return jpaRepository.findAllByCountry_Id(countryId).stream().map(mapper::toDomain).toList();
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
