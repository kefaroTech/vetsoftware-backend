package com.vetsoftware.app.medicament.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface MedicamentJpaRepository extends JpaRepository<MedicamentJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "company")
    List<MedicamentJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "company")
    Optional<MedicamentJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = "company")
    List<MedicamentJpaEntity> findAllByGeneralTrueOrCompany_Id(Long companyId);

    // Native: los pausados (enabled = false) NO pasan el @SQLRestriction; se listan crudos para reactivar.
    @Query(value = "SELECT * FROM medicaments WHERE enabled = false AND company_id = :companyId", nativeQuery = true)
    List<MedicamentJpaEntity> findAllDisabledForCompany(@Param("companyId") Long companyId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE medicaments SET enabled = true WHERE id = :id", nativeQuery = true)
    int reactivate(@Param("id") Long id);
}
