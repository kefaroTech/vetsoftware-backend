package com.vetsoftware.app.subscriptionbilling.application.port.in;

import com.vetsoftware.app.subscriptionbilling.application.dto.SubscriptionBillingBatchResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Devenga y factura el periodo que toca, <b>de todos los tenants</b>.
 *
 * <p>
 * <b>Hasta que existió este puerto, un contrato no facturaba solo.</b> El único
 * camino para generar una cuenta de cobro era el {@code POST} manual de la
 * consola de plataforma ({@code GenerateBillingDocumentUseCase}): alguien tenía
 * que acordarse, cada mes, de cada clínica.
 *
 * <p>
 * <b>Cerrado a {@code hasRole('SYSTEM')} a secas, y existe como puerto
 * precisamente para poder decirlo</b>
 * ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}). Un barrido de plataforma lee
 * filas de todas las empresas, así que solo lo puede servir un principal
 * cross-tenant. Escrito como método público de un {@code @Service} suelto —el
 * error que ya cometió {@code dunning}— su única protección sería que el
 * llamador se autentique antes, y eso es un hecho del árbol de llamadas, no una
 * propiedad del código: inyectar el bean en un controller nuevo bastaría para
 * facturarle a quien fuera.
 */
public interface RunSubscriptionBillingCycleUseCase {

    /**
     * Procesa el lote de contratos que sigue al cursor y devuelve el cursor de la
     * siguiente vuelta.
     *
     * @param runDateOffsetDays
     *            no existe: la fecha del barrido la pone el {@code Clock} inyectado
     *            en el servicio, nunca el llamador. Un job que pudiera elegir el
     *            día podría refacturar el mes pasado
     */
    @PreAuthorize("hasRole('SYSTEM')")
    SubscriptionBillingBatchResult processBatchAfter(long afterId, int batchSize);
}
