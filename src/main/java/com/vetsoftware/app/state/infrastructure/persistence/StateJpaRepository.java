package com.vetsoftware.app.state.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StateJpaRepository extends JpaRepository<StateJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "country")
    List<StateJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "country")
    Optional<StateJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = "country")
    List<StateJpaEntity> findAllByCountry_Id(Long countryId);
}
