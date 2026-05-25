package com.vetsoftware.app.laboratorytesttype.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LaboratoryTestTypeJpaRepository extends JpaRepository<LaboratoryTestTypeJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "company")
    List<LaboratoryTestTypeJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "company")
    Optional<LaboratoryTestTypeJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = "company")
    List<LaboratoryTestTypeJpaEntity> findAllByGeneralTrueOrCompany_Id(Long companyId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(
        value = "UPDATE laboratory_test_types SET enabled = true WHERE id = :id", nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);
}
