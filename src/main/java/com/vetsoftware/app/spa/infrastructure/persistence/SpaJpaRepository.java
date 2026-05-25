package com.vetsoftware.app.spa.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpaJpaRepository extends JpaRepository<SpaJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"spaType", "animal", "company"})
    List<SpaJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"spaType", "animal", "company"})
    Optional<SpaJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"spaType", "animal", "company"})
    List<SpaJpaEntity> findAllByAnimalId(Long animalId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(
        value = "UPDATE spas SET enabled = true WHERE id = :id", nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

    boolean existsBySpaType_Id(Long spaTypeId);

    boolean existsByAnimal_Id(Long animalId);

    boolean existsByCompany_Id(Long companyId);
}
