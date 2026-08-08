package com.vetsoftware.app.spa.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SpaJpaRepository extends JpaRepository<SpaJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"spaType", "animal", "company"})
    List<SpaJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"spaType", "animal", "company"})
    Optional<SpaJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"spaType", "animal", "company"})
    Optional<SpaJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    @EntityGraph(attributePaths = {"spaType", "animal", "company"})
    @Query("SELECT x FROM SpaJpaEntity x WHERE x.animal.id = :animalId "
            + "AND (:q IS NULL OR :q = '' OR LOWER(x.reason) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(x.details) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(x.observations) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<SpaJpaEntity> findAllByAnimalId(@Param("animalId") Long animalId, @Param("q") String q,
            Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = "UPDATE spas SET enabled = true WHERE id = :id", nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

    boolean existsBySpaType_Id(Long spaTypeId);

    boolean existsByAnimal_Id(Long animalId);

    boolean existsByCompany_Id(Long companyId);
}
