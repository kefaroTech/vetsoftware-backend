package com.vetsoftware.app.revenuerecognitionline.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <strong>Sin una sola {@code @Query}, y menos aun de {@code UPDATE} o
 * {@code DELETE}.</strong> Este libro solo se agrega: no hay ninguna escritura
 * sobre fila existente que {@code UPDATE_MASIVO_MUEVE_LA_VERSION} tuviera que
 * vigilar, y tampoco habria version que mover —la entidad esta exenta—. Un
 * {@code UPDATE} aqui reescribiria el ingreso de un periodo ya declarado sin
 * dejar rastro.
 *
 * <p>
 * <strong>Declara la variante acotada por empresa de las dos lecturas que la
 * admiten</strong> ({@code findByIdAndCompanyId} y {@code findAllByCompanyId}),
 * que es lo que exigen {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} y
 * {@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA} en una feature cuyas filas
 * pertenecen a alguien. {@code findAllByPostingPeriod} es la excepcion
 * deliberada: es el barrido del cierre mensual, y su caso de uso va cerrado a
 * {@code hasRole('SYSTEM')}.
 *
 * <p>
 * <strong>Sin {@code @EntityGraph}</strong>: la entidad no tiene ni una
 * asociacion —las tres claves foraneas son escalares— asi que no hay N+1 que
 * evitar.
 */
public interface RevenueRecognitionLineJpaRepository
        extends
            JpaRepository<RevenueRecognitionLineJpaEntity, Long> {

    Optional<RevenueRecognitionLineJpaEntity> findByIdAndCompanyId(Long id, Long companyId);

    Page<RevenueRecognitionLineJpaEntity> findAllByCompanyId(Long companyId, Pageable pageable);

    /**
     * <strong>Barrido de plataforma</strong>: sirve a {@code ix_rrl_period}, que no
     * lleva la empresa delante a proposito porque ponersela lo haria inutil para el
     * cierre mensual de todas las clinicas.
     */
    Page<RevenueRecognitionLineJpaEntity> findAllByPostingPeriod(String postingPeriod,
            Pageable pageable);
}
