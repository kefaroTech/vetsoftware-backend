package com.vetsoftware.app.publishadminpermissions.application.port.out;

import java.util.Map;
import java.util.Set;

/**
 * Que submodulos tiene concedidos cada empresa, en un solo viaje.
 *
 * <p>
 * Sustituye a {@code MembershipSubModuleIdsQueryPort}: la pregunta ya no es
 * «que abre el plan» sino «que abre el contrato». Se resuelve en lote —una
 * consulta para todas las empresas— porque la republicacion recorre el registro
 * entero y una consulta por empresa seria N+1 sobre la tabla mas caliente del
 * modelo.
 */
public interface CompanyGrantedSubModuleIdsQueryPort {
    Map<Long, Set<Long>> findGrantedSubModuleIdsByCompanyIds(Set<Long> companyIds);
}
