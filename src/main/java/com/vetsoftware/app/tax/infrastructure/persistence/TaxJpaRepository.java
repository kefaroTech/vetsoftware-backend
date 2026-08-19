package com.vetsoftware.app.tax.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxJpaRepository extends JpaRepository<TaxJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "company")
    List<TaxJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "company")
    Optional<TaxJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = "company")
    List<TaxJpaEntity> findAllByCompanyId(Long companyId);

    @EntityGraph(attributePaths = "company")
    Optional<TaxJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    // Query nativa: el @SQLRestriction("enabled = true") NO aplica a SQL nativo,
    // así que ésta es la
    // única vía para listar los impuestos PAUSADOS (enabled=false) y poder
    // reactivarlos desde la UI.
    @org.springframework.data.jpa.repository.Query(value = """
            SELECT *
            FROM taxes
            WHERE company_id = :companyId
              AND enabled = false
            ORDER BY name
            """, nativeQuery = true)
    List<TaxJpaEntity> findAllDisabledByCompany_Id(
            @org.springframework.data.repository.query.Param("companyId") Long companyId);

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
            UPDATE taxes
            SET enabled = true, version = version + 1
            WHERE id = :id
              AND company_id = :companyId
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);

    // @SQLRestriction("enabled = true") aplica: solo cuenta impuestos ACTIVOS (un
    // name desactivado se
    // reusa).
    boolean existsByCompany_IdAndName(Long companyId, String name);

    boolean existsByCompany_IdAndNameAndIdNot(Long companyId, String name, Long id);
}
