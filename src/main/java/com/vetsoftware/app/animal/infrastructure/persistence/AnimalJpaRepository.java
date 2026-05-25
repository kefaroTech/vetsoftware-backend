package com.vetsoftware.app.animal.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnimalJpaRepository extends JpaRepository<AnimalJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"specie", "breed", "owner", "company", "color"})
    List<AnimalJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"specie", "breed", "owner", "company", "color"})
    Optional<AnimalJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"specie", "breed", "owner", "company", "color"})
    List<AnimalJpaEntity> findAllByOwner_IdAndCompany_Id(Long ownerId, Long companyId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(
        value = "UPDATE animals SET enabled = true WHERE id = :id",
        nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

    boolean existsByColor_Id(Long colorId);

    boolean existsBySpecie_Id(Long specieId);

    boolean existsByBreed_Id(Long breedId);

    boolean existsByOwner_Id(Long ownerId);

    boolean existsByCompany_Id(Long companyId);
}
