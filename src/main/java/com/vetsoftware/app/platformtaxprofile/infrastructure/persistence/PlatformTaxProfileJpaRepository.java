package com.vetsoftware.app.platformtaxprofile.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * <strong>Las tres consultas llevan {@code @EntityGraph} sobre
 * {@code economicActivity}</strong>, que es obligatorio con la asociacion
 * {@code LAZY}: sin el, listar diez fichas del historico dispara once consultas
 * y el N+1 no se ve hasta que el historico crece.
 *
 * <p>
 * <strong>Aqui el listado ancho SI es legitimo, al reves que en
 * {@code companybillingprofile}.</strong> {@code platform_tax_profiles} es una
 * tabla global sin {@code company_id}: no hay filas «de otro tenant» que un
 * filtro pudiera proteger, y la unica lectura posible es la completa. Lo que
 * cierra el acceso es el {@code @PreAuthorize("hasRole('SYSTEM')")} de los
 * puertos de entrada, que es exactamente lo que exige
 * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} cuando el puerto no transporta
 * empresa.
 *
 * <p>
 * <strong>Sin una sola {@code @Query} de {@code UPDATE} o
 * {@code DELETE}.</strong> Toda escritura pasa por el ciclo
 * leer-modificar-guardar sobre entidad gestionada, que es el unico camino que
 * {@code @Version} protege, asi que aqui no hay SQL que pudiera olvidarse de
 * mover la version en su {@code SET} ({@code UPDATE_MASIVO_MUEVE_LA_VERSION},
 * #53). Y no hay borrado de ninguna clase: una identidad se cierra con
 * {@code valid_to} y queda.
 */
public interface PlatformTaxProfileJpaRepository
        extends
            JpaRepository<PlatformTaxProfileJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "economicActivity")
    Optional<PlatformTaxProfileJpaEntity> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "economicActivity")
    Page<PlatformTaxProfileJpaEntity> findAll(Pageable pageable);

    /**
     * La identidad vigente: {@code valid_to} nulo. Recorre
     * {@code ix_platform_tax_profiles_current (valid_to)}.
     *
     * <p>
     * Devuelve {@code Optional} y no {@code List} porque la unicidad no la sostiene
     * esta consulta: la impone {@code uq_platform_tax_profiles_current} sobre la
     * columna generada. Si alguna vez llegaran dos, lo correcto es que Spring Data
     * lance en vez de que el servicio elija una en silencio — y «elegir una en
     * silencio» aqui significaria imprimir una razon social u otra en las facturas
     * segun el plan que eligiera el motor.
     */
    @EntityGraph(attributePaths = "economicActivity")
    @Query("""
            SELECT p
            FROM PlatformTaxProfileJpaEntity p
            WHERE p.validTo IS NULL
            """)
    Optional<PlatformTaxProfileJpaEntity> findCurrent();
}
