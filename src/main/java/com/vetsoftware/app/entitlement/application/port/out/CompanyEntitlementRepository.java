package com.vetsoftware.app.entitlement.application.port.out;

import com.vetsoftware.app.entitlement.domain.CompanyEntitlement;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;

/**
 * Puerto de salida de los permisos derivados.
 *
 * <p>
 * <strong>Toda operacion esta acotada por empresa, sin excepcion.</strong> No
 * hay {@code findById} ni {@code findAll}: un permiso derivado no se consulta
 * suelto --se consulta el estado completo de una empresa-- y una lectura sin
 * empresa devolveria filas de todos los tenants.
 */
public interface CompanyEntitlementRepository {

    /**
     * Todos los permisos de la empresa, caducados incluidos. Point lookup por el
     * prefijo {@code company_id} de {@code uq_company_entitlements}.
     */
    List<CompanyEntitlement> findAllByCompanyId(Long companyId);

    /** El mismo listado paginado, para la vista de auditoria. */
    PageResult<CompanyEntitlement> findPageByCompanyId(Long companyId, int page, int pageSize);

    /**
     * Las concesiones manuales de la empresa: las unicas filas que el recalculo
     * <strong>no</strong> puede reconstruir, porque no salen de ningun contrato. El
     * recalculo las lee antes de borrar para excluir sus submodulos del calculo y
     * no chocar contra {@code uq_company_entitlements}.
     */
    List<CompanyEntitlement> findManualGrantsByCompanyId(Long companyId);

    List<CompanyEntitlement> saveAll(List<CompanyEntitlement> entitlements);

    /**
     * Borrado <strong>fisico</strong> de los permisos <strong>derivados</strong> de
     * una empresa --{@code SUBSCRIPTION}, {@code TRIAL}, {@code CORE}--, y es la
     * unica excepcion de borrado fisico de todo el modelo de suscripciones: esas
     * filas se reconstruyen enteras desde el contrato y no hay nada que conservar.
     * Por eso la tabla tampoco lleva {@code enabled}.
     *
     * <p>
     * <strong>Las {@code MANUAL_GRANT} quedan fuera del borrado.</strong> Una
     * concesion a mano no es derivable, asi que borrarla en el recalculo la haria
     * desaparecer sin que nadie pudiera reconstruirla --y disparada por un cambio
     * en otra linea del contrato, que nadie relacionaria con ella--.
     *
     * @return filas borradas
     */
    int deleteDerivedByCompanyId(Long companyId);
}
