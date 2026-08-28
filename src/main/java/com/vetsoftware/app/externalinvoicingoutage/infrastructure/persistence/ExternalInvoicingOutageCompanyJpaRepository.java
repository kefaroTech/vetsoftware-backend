package com.vetsoftware.app.externalinvoicingoutage.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <strong>{@code @EntityGraph} obligatorio en las lecturas</strong>: la
 * asociacion a la caida es {@code LAZY} y sin el grafo, listar el reparto
 * dispara una consulta por fila. La asociacion es {@code to-one}, asi que el
 * {@code JOIN FETCH} convive con la paginacion sin traerse la tabla al heap
 * (HHH000104).
 *
 * <p>
 * <strong>Sin {@code delete} de ningun tipo</strong> —tampoco el heredado se
 * usa desde el adaptador—: quitar una clinica del reparto destruye la prueba de
 * que se le aviso.
 *
 * <p>
 * <strong>Sin {@code @Query}.</strong> Las tres consultas son derivadas, lo que
 * de paso deja fuera de alcance a {@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA} y
 * a {@code PROYECCION_SIN_LITERAL_BOOLEANO}: el {@code exists} derivado lo
 * resuelve Spring Data con un {@code SELECT count(...)}, no proyectando un
 * literal booleano que Hibernate 7 tiparia como {@code Integer}.
 */
public interface ExternalInvoicingOutageCompanyJpaRepository
        extends
            JpaRepository<ExternalInvoicingOutageCompanyJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "outage")
    Optional<ExternalInvoicingOutageCompanyJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = "outage")
    Page<ExternalInvoicingOutageCompanyJpaEntity> findByOutage_Id(Long outageId, Pageable pageable);

    boolean existsByOutage_IdAndCompanyId(Long outageId, Long companyId);
}
