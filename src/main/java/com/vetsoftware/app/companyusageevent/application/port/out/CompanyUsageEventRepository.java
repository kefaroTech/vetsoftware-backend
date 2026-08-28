package com.vetsoftware.app.companyusageevent.application.port.out;

import com.vetsoftware.app.companyusageevent.domain.CompanyUsageEvent;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.Optional;

/**
 * <strong>Todos los listados de este puerto reciben {@code companyId} menos
 * uno, y ese uno es el barrido del cierre.</strong> Es la forma que
 * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} (BE-29) obliga a declarar: como el
 * repositorio <em>sabe</em> filtrar por empresa, cualquier {@code find...} suyo
 * que devuelva varias filas sin ese filtro solo lo puede servir un caso de uso
 * cerrado a {@code hasRole('SYSTEM')} a secas. {@link #findAll} lo esta.
 *
 * <p>
 * <strong>Ni un metodo devuelve {@code List}.</strong> La proyeccion de esta
 * tabla son ~12 millones de filas a diez anos y quinientas clinicas: un
 * {@code findAll()} que devolviera una lista seria un {@code SELECT *} sobre
 * eso, y el dia que reventara lo haria en el proceso de cierre, de noche.
 */
public interface CompanyUsageEventRepository {

    CompanyUsageEvent save(CompanyUsageEvent event);

    /**
     * Carga ancha, para el servicio que solo alcanza {@code SYSTEM}: un principal
     * de plataforma no tiene empresa contra la que acotar.
     */
    Optional<CompanyUsageEvent> findById(Long id);

    /**
     * La variante acotada, y la que hay que usar en cuanto haya un
     * {@code companyId} a mano ({@code CARGA_POR_ID_ACOTADA_POR_EMPRESA}). Es la
     * unica que impide que el cierre de una clinica toque el hecho de otra.
     */
    Optional<CompanyUsageEvent> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * El barrido de plataforma. Sin empresa a proposito: ver el javadoc del tipo.
     */
    PageResult<CompanyUsageEvent> findAll(int page, int pageSize);

    PageResult<CompanyUsageEvent> findAllByCompanyId(Long companyId, int page, int pageSize);

    /**
     * El desglose de un cargo, acotado por empresa. Lo sirve {@code ix_cue_charge}.
     */
    PageResult<CompanyUsageEvent> findAllByCompanyIdAndChargeId(Long companyId, Long chargeId,
            int page, int pageSize);
}
