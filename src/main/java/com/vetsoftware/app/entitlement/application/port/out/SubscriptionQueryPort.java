package com.vetsoftware.app.entitlement.application.port.out;

import com.vetsoftware.app.entitlement.domain.ContractSnapshot;
import java.time.LocalDate;
import java.util.Optional;

/**
 * La unica puerta por la que este slice mira el contrato de una empresa. El
 * adaptador que la implementa es el unico archivo que conoce las tablas de
 * {@code subscription} y {@code catalogitem}.
 *
 * <p>
 * Las dos consultas van acotadas por empresa --nunca por id de contrato a
 * secas--: resolver una referencia a otra feature sin el {@code companyId} es
 * exactamente la fuga que cierra
 * {@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA}.
 */
public interface SubscriptionQueryPort {

    /**
     * El contrato vigente ese dia: <strong>ya empezo y todavia no ha
     * terminado</strong>. Incluye {@code PAST_DUE} y {@code READ_ONLY}, que siguen
     * siendo contratos vigentes; deja fuera {@code CANCELLED} y {@code EXPIRED}.
     */
    Optional<ContractSnapshot> findCurrentContractByCompanyId(Long companyId, LocalDate on);

    /**
     * El ultimo contrato de la empresa sea cual sea su estado. Es lo que permite
     * que una empresa cuyo contrato se cancelo conserve el acceso de solo lectura a
     * lo que ya escribio en vez de quedarse sin nada.
     */
    Optional<ContractSnapshot> findLatestContractByCompanyId(Long companyId);
}
