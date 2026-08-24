package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import com.vetsoftware.app.electronicdocument.application.port.out.BillingEntitlementQueryPort;
import com.vetsoftware.app.entitlement.infrastructure.persistence.CompanyEntitlementJpaRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Resuelve el derecho a facturación electrónica: ¿el contrato vigente de esta
 * empresa le concede el submódulo {@code BILLING} con acceso completo?
 *
 * <p>
 * Antes el salto era empresa → membresía → {@code membership_sub_modules}.
 * Ahora es empresa → {@code company_entitlements}, que es la tabla derivada del
 * contrato: un solo *point lookup* por {@code (company_id, sub_module_id)}, sin
 * pasar por la empresa.
 *
 * <p>
 * <b>Dos cosas que no se pueden "simplificar" de vuelta, y las dos tienen
 * historia.</b> El método al que esto sustituye tuvo la facturación caída al
 * 100 % (incidencia #185). Y la regla dura
 * {@code PROYECCION_SIN_LITERAL_BOOLEANO} (#196) nació de ese mismo método, por
 * proyectar un literal booleano en el {@code SELECT}: por eso el repositorio
 * devuelve un {@code long} y la comparación con cero vive aquí, en Java, y no
 * en el SQL.
 *
 * <p>
 * Solo {@code FULL} cuenta. {@code READ_ONLY} es consulta e impresión, no
 * crear: emitir una factura ante la DIAN es exactamente crear, así que un
 * módulo de facturación dado de baja no puede seguir emitiendo.
 */
@Component
public class JpaBillingEntitlementQueryPort implements BillingEntitlementQueryPort {

    private static final String BILLING_CODE = "BILLING";
    private static final List<String> ACCESO_COMPLETO = List.of("FULL");

    private final CompanyEntitlementJpaRepository companyEntitlementJpaRepository;

    public JpaBillingEntitlementQueryPort(
            CompanyEntitlementJpaRepository companyEntitlementJpaRepository) {
        this.companyEntitlementJpaRepository = companyEntitlementJpaRepository;
    }

    @Override
    public boolean isElectronicInvoicingEnabled(Long companyId) {
        if (companyId == null)
            return false;
        return companyEntitlementJpaRepository.countGrantedByCompanyIdAndSubModuleCode(companyId,
                BILLING_CODE, ACCESO_COMPLETO) > 0;
    }
}
