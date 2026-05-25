package com.vetsoftware.app.city.infrastructure.persistence;

import com.vetsoftware.app.city.application.port.out.CityRepository;
import com.vetsoftware.app.city.domain.City;
import com.vetsoftware.app.state.infrastructure.persistence.StateJpaEntity;
import com.vetsoftware.app.state.infrastructure.persistence.StateJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCityRepository implements CityRepository {
    private final CityJpaRepository jpaRepository;
    private final CityJpaMapper mapper;
    private final StateJpaRepository stateJpaRepository;

    public JpaCityRepository(CityJpaRepository jpaRepository,
                              CityJpaMapper mapper,
                              StateJpaRepository stateJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.stateJpaRepository = stateJpaRepository;
    }

    @Override
    public City save(City city) {
        StateJpaEntity state = stateJpaRepository.getReferenceById(city.getState().id());
        CityJpaEntity saved = jpaRepository.save(mapper.toJpa(city, state));
        return mapper.toDomain(saved, city.getState());
    }

    @Override
    public Optional<City> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<City> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<City> findByStateId(Long stateId) {
        return jpaRepository.findAllByState_Id(stateId).stream().map(mapper::toDomain).toList();
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
