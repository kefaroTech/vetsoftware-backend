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
     * El dia en que la empresa firmo su contrato
     * ({@code subscriptions.start_date}), sin traer nada mas.
     *
     * <p>
     * Existe aparte del par de arriba porque su unico consumidor es la decision de
     * D-74 en el camino de consumo, y ese camino no puede permitirse las tres
     * consultas que arma un {@code ContractSnapshot} completo --cabecera, lineas de
     * modulo y lineas de capacidad-- para leer una fecha. Se invoca ademas solo en
     * la rama en la que el contador ya iba a fallar, asi que ni siquiera esa
     * consulta esta en el camino feliz.
     *
     * @return vacio si la empresa no tiene ningun contrato. Sin firma no hay nada
     *         anterior a lo que ampararse, y la regla vieja --sin fila, techo
     *         cero-- sigue siendo la respuesta correcta
     */
    Optional<LocalDate> findContractSignedOnByCompanyId(Long companyId);

    /**
     * Toma el candado del contrato de una empresa antes de tocar nada suyo.
     *
     * <p>
     * <strong>El contrato primero, siempre</strong> (R-ENT-08). Es el orden de
     * bloqueo que hace que dos recalculos simultaneos de la misma empresa se pongan
     * en fila en vez de chocar contra {@code uq_company_entitlements}, y que un
     * otrosi confirmado en mitad de un recalculo no acabe en un reinsert sin la
     * linea nueva.
     */
    void lockContractByCompanyId(Long companyId);

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
