package com.vetsoftware.app.service.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ServiceJpaRepository
        extends
            JpaRepository<ServiceJpaEntity, Long>,
            JpaSpecificationExecutor<ServiceJpaEntity> {

    @Override
    @EntityGraph(attributePaths = {"serviceCategory", "tax", "company"})
    List<ServiceJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"serviceCategory", "tax", "company"})
    Optional<ServiceJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"serviceCategory", "tax", "company"})
    List<ServiceJpaEntity> findAllByCompanyId(Long companyId);

    @EntityGraph(attributePaths = {"serviceCategory", "tax", "company"})
    Optional<ServiceJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    // Query nativa: el @SQLRestriction("enabled = true") NO aplica a SQL nativo,
    // así que ésta es la
    // única vía para listar los servicios PAUSADOS (enabled=false) y poder
    // reactivarlos desde la UI.
    @org.springframework.data.jpa.repository.Query(value = """
            SELECT *
            FROM services
            WHERE company_id = :companyId
              AND enabled = false
            ORDER BY name
            """, nativeQuery = true)
    List<ServiceJpaEntity> findAllDisabledByCompany_Id(
            @org.springframework.data.repository.query.Param("companyId") Long companyId);

    // El UPDATE mueve tambien `version`, la del bloqueo
    // optimista, a proposito: sin eso, un save cargado antes
    // de la reactivacion reescribe `enabled` con su valor
    // viejo —el mapper lo copia desde el dominio— y su
    // WHERE version = ? casa igual, con lo que una edicion
    // concurrente vuelve a apagar en silencio lo que la
    // reactivacion acababa de encender. Movida la version,
    // ese save ya no encuentra fila y salta
    // ObjectOptimisticLockingFailureException -> 409
    // CONCURRENT_MODIFICATION. `version` NO va en el WHERE:
    // reactivar es una operacion deliberada y debe
    // ejecutarse siempre, no competir con una edicion.
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE services
            SET enabled = true, version = version + 1
            WHERE id = :id
              AND company_id = :companyId
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);

    boolean existsByTax_Id(Long taxId);

    boolean existsByServiceCategory_Id(Long serviceCategoryId);

    boolean existsByIdAndCompany_Id(Long id, Long companyId);
}
