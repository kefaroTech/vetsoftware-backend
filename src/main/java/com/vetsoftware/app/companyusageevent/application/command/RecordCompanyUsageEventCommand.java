package com.vetsoftware.app.companyusageevent.application.command;

import java.time.LocalDateTime;

/**
 * Anotar un hecho de consumo.
 *
 * <p>
 * <strong>Lleva {@code companyId} y es la consola de plataforma quien lo
 * elige.</strong> Un principal {@code SYSTEM} no tiene empresa propia: la
 * empresa a la que se le anota el consumo viaja como {@code @RequestParam}
 * —nunca en el cuerpo, {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}— y el controller
 * la inyecta aqui.
 *
 * @param occurredAt
 *            <strong>el instante del registro consumido, NO el del reloj del
 *            proceso.</strong> Es el campo del que depende {@code uq_cue_fact}:
 *            rellenarlo con la hora actual hace que el reintento del medidor
 *            deje de chocar y duplique el hecho —y con el, el excedente
 *            facturado— sin un solo error. La hora en que se anota la pone el
 *            {@code Clock} inyectado en {@code created_date}, que es otra
 *            columna y otro instante
 * @param limitDimensionCode
 *            el codigo del eje. Solo los cuatro contables ({@code OWNER},
 *            {@code ANIMAL}, {@code APPOINTMENT}, {@code INVOICE}) acumulan
 *            hechos; los de existencias se cuentan contra su propia tabla
 * @param usageReferenceId
 *            el identificador del registro consumido en la tabla que
 *            corresponde al eje. Va a una de las cuatro columnas de rama
 * @param periodKey
 *            {@code AAAA-MM}, {@code AAAA-Qn}, {@code AAAA-Sn} o
 *            {@code ALLTIME}
 * @param billable
 *            si el hecho cuenta para el cobro. Un hecho no facturable no puede
 *            colgarse despues de un cargo
 */
public record RecordCompanyUsageEventCommand(Long companyId, String limitDimensionCode,
        Long usageReferenceId, LocalDateTime occurredAt, String periodKey, boolean billable) {
}
