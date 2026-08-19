package com.vetsoftware.app.product.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductJpaRepository
        extends
            JpaRepository<ProductJpaEntity, Long>,
            JpaSpecificationExecutor<ProductJpaEntity> {

    @Override
    @EntityGraph(attributePaths = {"productCategory", "tax", "company", "supplier"})
    List<ProductJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"productCategory", "tax", "company", "supplier"})
    Optional<ProductJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"productCategory", "tax", "company", "supplier"})
    List<ProductJpaEntity> findAllByCompanyId(Long companyId);

    @EntityGraph(attributePaths = {"productCategory", "tax", "company", "supplier"})
    Optional<ProductJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    // Query nativa: el @SQLRestriction("enabled = true") NO aplica a SQL nativo,
    // así que ésta es la
    // única vía para listar los productos PAUSADOS (enabled=false) y poder
    // reactivarlos desde la UI.
    // Las asociaciones se hidratan perezosamente dentro de la transacción de
    // lectura del caso de uso.
    @org.springframework.data.jpa.repository.Query(value = """
            SELECT *
            FROM products
            WHERE company_id = :companyId
              AND enabled = false
            ORDER BY name
            """, nativeQuery = true)
    List<ProductJpaEntity> findAllDisabledByCompany_Id(
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
            UPDATE products
            SET enabled = true, version = version + 1
            WHERE id = :id
              AND company_id = :companyId
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);

    boolean existsByTax_Id(Long taxId);

    boolean existsByProductCategory_Id(Long productCategoryId);

    boolean existsByIdAndCompany_Id(Long id, Long companyId);

    // @SQLRestriction("enabled = true") aplica: solo cuenta productos ACTIVOS (un
    // code desactivado se
    // reusa).
    boolean existsByCompany_IdAndCode(Long companyId, String code);

    boolean existsByCompany_IdAndCodeAndIdNot(Long companyId, String code, Long id);

    // @SQLRestriction("enabled = true") aplica: solo cuenta productos ACTIVOS (un
    // name desactivado se
    // reusa).
    boolean existsByCompany_IdAndName(Long companyId, String name);

    boolean existsByCompany_IdAndNameAndIdNot(Long companyId, String name, Long id);
}
