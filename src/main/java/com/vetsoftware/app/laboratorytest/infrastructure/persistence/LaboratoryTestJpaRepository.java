package com.vetsoftware.app.laboratorytest.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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
    List<LaboratoryTestJpaEntity> findAllByAnimalId(Long animalId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = "UPDATE laboratory_tests SET enabled = true WHERE id = :id", nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

    boolean existsByTestType_Id(Long testTypeId);

    boolean existsByAnimal_Id(Long animalId);

    boolean existsByConsultation_Id(Long consultationId);

    boolean existsByCompany_Id(Long companyId);
}
