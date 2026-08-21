package com.vetsoftware.app.surgerytype.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurgeryTypeJpaRepository extends JpaRepository<SurgeryTypeJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "company")
    List<SurgeryTypeJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "company")
    Optional<SurgeryTypeJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = "company")
    @org.springframework.data.jpa.repository.Query("""
            SELECT e
            FROM SurgeryTypeJpaEntity e
            LEFT
            JOIN e.company c
            WHERE e.id = :id
              AND (e.general = true OR c.id = :companyId)
            """)
    Optional<SurgeryTypeJpaEntity> findAvailableById(
            @org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);

    @EntityGraph(attributePaths = "company")
    List<SurgeryTypeJpaEntity> findAllByGeneralTrueOrCompany_Id(Long companyId);

    /**
     * Lectura ESTRICTA por propiedad, para los caminos de ESCRITURA. A diferencia
     * de {@link #findAvailableById}, que incluye a propósito las filas generales
     * porque sirve a los {@code find}/{@code list}, esta excluye lo que la empresa
     * solo puede consultar: editar, borrar o reactivar una fila general la
     * cambiaría para todos los tenants, y una fila general ajena la reasignaría.
     */
    @EntityGraph(attributePaths = "company")
    Optional<SurgeryTypeJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);
}
