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

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = "UPDATE states SET enabled = true WHERE id = :id", nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

    boolean existsByCountry_Id(Long countryId);
}
