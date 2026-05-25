package com.vetsoftware.app.country.infrastructure.persistence;

import com.vetsoftware.app.country.application.port.out.CountryRepository;
import com.vetsoftware.app.country.domain.Country;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCountryRepository implements CountryRepository {
    private final CountryJpaRepository jpaRepository;
    private final CountryJpaMapper mapper;

    public JpaCountryRepository(CountryJpaRepository jpaRepository, CountryJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Country save(Country country) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(country)));
    }

    @Override
    public Optional<Country> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Country> findAll() {
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
