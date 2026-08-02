package com.vetsoftware.app.branch.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BranchJpaRepository extends JpaRepository<BranchJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"city", "company"})
    Optional<BranchJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"city", "company"})
    @Query("SELECT b FROM BranchJpaEntity b WHERE b.id = :id AND b.company.id = :companyId")
    Optional<BranchJpaEntity> findByIdAndCompanyId(@Param("id") Long id,
            @Param("companyId") Long companyId);

    @EntityGraph(attributePaths = {"city", "company"})
    @Query("SELECT b FROM BranchJpaEntity b WHERE b.company.id = :companyId ORDER BY b.name")
    List<BranchJpaEntity> findAllByCompanyId(@Param("companyId") Long companyId);

    boolean existsByCompany_IdAndCodeIgnoreCase(Long companyId, String code);

    boolean existsByCompany_IdAndCodeIgnoreCaseAndIdNot(Long companyId, String code, Long id);

    // ¿Existe la sede en la empresa, esté activa o no? Sirve para distinguir
    // "inactiva" de
    // "inexistente"
    // al resolver una escritura (mensaje de error preciso), sin cargar la entidad.
    boolean existsByIdAndCompany_Id(Long id, Long companyId);

    // ¿Hay OTRA sede activa en la empresa (distinta de :id)? Guarda contra
    // desactivar la última sede
    // activa.
    boolean existsByCompany_IdAndActiveTrueAndIdNot(Long companyId, Long id);

    // Sede por defecto de una empresa (resolución de branchId cuando el request no
    // lo trae): la
    // "Principal".
    Optional<BranchJpaEntity> findFirstByCompany_IdAndCodeIgnoreCase(Long companyId, String code);

    // Sede por defecto para ESCRIBIR: la "Principal" pero solo si está ACTIVA. Si
    // la Principal fue
    // desactivada/renombrada se cae al siguiente fallback; una sede inactiva nunca
    // es la sede por
    // defecto.
    Optional<BranchJpaEntity> findFirstByCompany_IdAndCodeIgnoreCaseAndActiveTrue(Long companyId,
            String code);

    // Fallback si no hubiese "Principal" (p. ej. renombrada): la primera sede
    // activa.
    Optional<BranchJpaEntity> findFirstByCompany_IdAndActiveTrueOrderByIdAsc(Long companyId);
}
