package com.vetsoftware.app.laboratorytest.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LaboratoryTestJpaRepository
        extends
            JpaRepository<LaboratoryTestJpaEntity, Long>,
            JpaSpecificationExecutor<LaboratoryTestJpaEntity> {

    @Override
    @EntityGraph(attributePaths = {"testType", "animal", "consultation", "company", "processedBy"})
    List<LaboratoryTestJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"testType", "animal", "consultation", "company", "processedBy"})
    Optional<LaboratoryTestJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"testType", "animal", "consultation", "company", "processedBy"})
    Optional<LaboratoryTestJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    @EntityGraph(attributePaths = {"testType", "animal", "consultation", "company", "processedBy"})
    @Query("""
            SELECT x
            FROM LaboratoryTestJpaEntity x
            WHERE x.animal.id = :animalId
              AND x.company.id = :companyId
              AND (:q IS NULL OR :q = '' OR LOWER(x.diagnosis) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<LaboratoryTestJpaEntity> findAllByAnimalIdAndCompanyId(@Param("animalId") Long animalId,
            @Param("companyId") Long companyId, @Param("q") String q, Pageable pageable);

    boolean existsByTestType_Id(Long testTypeId);

    boolean existsByAnimal_Id(Long animalId);

    boolean existsByConsultation_Id(Long consultationId);

    boolean existsByCompany_Id(Long companyId);
}
