package com.vetsoftware.app.companyactivitymonth.application.command;

import com.vetsoftware.app.companyactivitymonth.domain.CommercialState;
import java.math.BigDecimal;

/**
 * Da de alta la fila de actividad de una clinica en un mes.
 *
 * <p>
 * <strong>Lleva {@code companyId} y el controller lo toma de la query string,
 * no del cuerpo.</strong> Esta es una ruta de plataforma: un principal
 * {@code SYSTEM} no tiene empresa propia y tiene que elegir a que clinica se
 * refiere la fila. La empresa viaja como {@code @RequestParam} porque
 * {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} prohibe el cuerpo <em>y solo el
 * cuerpo</em>: un {@code companyId} escrito en el JSON convertiria cualquier
 * comprobacion de tenant en una comparacion del numero consigo mismo.
 *
 * @param periodKey
 *            el mes en formato {@code AAAA-MM}. Se valida al construir el
 *            {@code ActivityPeriodKey} del dominio, que es donde vive el
 *            {@code REGEXP}
 * @param activeDays
 *            dias del mes con al menos un acceso. Entre 0 y los dias que tenga
 *            ese mes
 * @param mrrSnapshot
 *            el MRR <b>ya normalizado a mensual</b>, con dos decimales como
 *            maximo. Cero es legitimo: un mes en prueba o ya de baja no factura
 */
public record RecordCompanyActivityMonthCommand(Long companyId, String periodKey,
        CommercialState commercialState, int activeDays, int activeUsers, int recordsCreated,
        BigDecimal mrrSnapshot) {
}
