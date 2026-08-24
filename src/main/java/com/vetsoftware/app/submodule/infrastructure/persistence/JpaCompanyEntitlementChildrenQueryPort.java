package com.vetsoftware.app.submodule.infrastructure.persistence;

import com.vetsoftware.app.entitlement.infrastructure.persistence.CompanyEntitlementJpaRepository;
import com.vetsoftware.app.submodule.application.port.out.CompanyEntitlementChildrenQueryPort;
import org.springframework.stereotype.Component;

/**
 * Adaptador de la guarda del lado del inquilino (#413).
 *
 * <p>
 * <b>Aqui si se inyecta el repositorio Spring Data de la otra feature</b>, al
 * reves que su vecino {@code JpaCatalogItemChildrenQueryPort}, que resuelve la
 * suya con SQL nativo por {@code EntityManager}. No es una incoherencia: aquel
 * apunta a {@code catalog_item_sub_modules}, cuyo slice se estaba escribiendo
 * en paralelo y no tenia repositorio al que llamar (#380 sigue esa deuda).
 * {@code CompanyEntitlementJpaRepository} lleva meses en el arbol y ya lo
 * consumen asi otros tres slices --{@code electronicdocument},
 * {@code publishadminpermissions} y {@code registration}--, asi que este es el
 * patron de la casa y no una excepcion: mantiene la consulta visible para las
 * reglas de arquitectura, que es justo lo que el SQL crudo pierde.
 *
 * <p>
 * La comparacion con cero se hace aqui y no en el {@code SELECT}:
 * {@code PROYECCION_SIN_LITERAL_BOOLEANO} (#196).
 */
@Component
public class JpaCompanyEntitlementChildrenQueryPort implements CompanyEntitlementChildrenQueryPort {

    private final CompanyEntitlementJpaRepository companyEntitlementJpaRepository;

    public JpaCompanyEntitlementChildrenQueryPort(
            CompanyEntitlementJpaRepository companyEntitlementJpaRepository) {
        this.companyEntitlementJpaRepository = companyEntitlementJpaRepository;
    }

    @Override
    public boolean existsActiveBySubModuleId(Long subModuleId) {
        return companyEntitlementJpaRepository.countActiveBySubModuleId(subModuleId) > 0;
    }
}
