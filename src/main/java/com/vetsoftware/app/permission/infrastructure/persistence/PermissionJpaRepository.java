package com.vetsoftware.app.permission.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionJpaRepository extends JpaRepository<PermissionJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"company", "subModule"})
    List<PermissionJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"company", "subModule"})
    Optional<PermissionJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"company", "subModule"})
    Optional<PermissionJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    Optional<PermissionJpaEntity> findByCompanyIdAndCode(Long companyId, String code);

    @EntityGraph(attributePaths = {"company", "subModule"})
    List<PermissionJpaEntity> findAllByCompanyId(Long companyId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE permissions
            SET enabled = true
            WHERE id = :id
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

    /**
     * Reactivacion acotada al tenant. El gate del puerto ya es
     * {@code hasRole('SYSTEM')}, pero la reactivacion no tiene lectura previa que
     * valide la propiedad: este {@code AND company_id} es lo que impide que una
     * empresa seleccionada por error alcance el permiso de otra, y lo que deja la
     * barrera en el SQL si algun dia se reabre el gate al tenant.
     */
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE permissions
            SET enabled = true
            WHERE id = :id
              AND company_id = :companyId
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);

    @org.springframework.data.jpa.repository.Query(value = """
            SELECT id
            FROM permissions
            WHERE company_id = :companyId
              AND code = :code
              AND enabled = false
            LIMIT 1
            """, nativeQuery = true)
    Optional<Long> findDisabledIdByCompanyIdAndCode(
            @org.springframework.data.repository.query.Param("companyId") Long companyId,
            @org.springframework.data.repository.query.Param("code") String code);

    boolean existsByCompany_Id(Long companyId);

    boolean existsBySubModule_Id(Long subModuleId);
}
