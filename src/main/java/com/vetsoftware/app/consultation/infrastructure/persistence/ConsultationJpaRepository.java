package com.vetsoftware.app.consultation.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultationJpaRepository extends JpaRepository<ConsultationJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"consultationType", "animal", "company"})
    List<ConsultationJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"consultationType", "animal", "company"})
    Optional<ConsultationJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"consultationType", "animal", "company"})
    Optional<ConsultationJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    @EntityGraph(attributePaths = {"consultationType", "animal", "company"})
    Page<ConsultationJpaEntity> findAllByCompany_Id(Long companyId, Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = "UPDATE consultations SET enabled = true WHERE id = :id AND company_id = :companyId", nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);

    boolean existsByConsultationType_Id(Long consultationTypeId);

    boolean existsByAnimal_Id(Long animalId);

    boolean existsByCompany_Id(Long companyId);
}
