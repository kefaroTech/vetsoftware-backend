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
}
