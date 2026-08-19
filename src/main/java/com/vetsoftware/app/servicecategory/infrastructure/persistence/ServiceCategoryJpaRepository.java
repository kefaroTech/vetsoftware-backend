package com.vetsoftware.app.servicecategory.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceCategoryJpaRepository
        extends
            JpaRepository<ServiceCategoryJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "company")
    List<ServiceCategoryJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "company")
    Optional<ServiceCategoryJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = "company")
    List<ServiceCategoryJpaEntity> findAllByCompany_Id(Long companyId);

    @EntityGraph(attributePaths = "company")
    Optional<ServiceCategoryJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    // Sube version porque este UPDATE nativo va directo a la base: no comprueba
    // ni incrementa la version, que @Version solo protege en el ciclo
    // leer-modificar-guardar. Un save concurrente cargado antes reescribe la
    // fila entera desde el dominio, con su enabled = false, y su
    // WHERE version = ? casa igual, deshaciendo la reactivacion en silencio.
    // Movida la version, ese save ya no encuentra fila y salta
    // ObjectOptimisticLockingFailureException -> 409 CONCURRENT_MODIFICATION.
    // La version NO va en el WHERE: reactivar es deliberado y debe ejecutarse
    // siempre, no competir con una edicion.
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE service_categories
            SET enabled = true, version = version + 1
            WHERE id = :id
              AND company_id = :companyId
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);

    boolean existsByIdAndCompany_Id(Long id, Long companyId);

    // @SQLRestriction("enabled = true") aplica: solo cuenta categorías ACTIVAS (un
    // name desactivado
    // se reusa).
    boolean existsByCompany_IdAndName(Long companyId, String name);

    boolean existsByCompany_IdAndNameAndIdNot(Long companyId, String name, Long id);
}
