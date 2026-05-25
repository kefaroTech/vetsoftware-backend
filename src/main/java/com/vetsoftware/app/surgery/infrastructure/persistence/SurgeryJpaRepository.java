package com.vetsoftware.app.surgery.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurgeryJpaRepository extends JpaRepository<SurgeryJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"surgeryType", "animal", "consultation", "company"})
    List<SurgeryJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"surgeryType", "animal", "consultation", "company"})
    Optional<SurgeryJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"surgeryType", "animal", "consultation", "company"})
    List<SurgeryJpaEntity> findAllByAnimalId(Long animalId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(
        value = "UPDATE surgeries SET enabled = true WHERE id = :id",
        nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

    boolean existsBySurgeryType_Id(Long surgeryTypeId);

    boolean existsByAnimal_Id(Long animalId);

    boolean existsByConsultation_Id(Long consultationId);

    boolean existsByCompany_Id(Long companyId);
}
