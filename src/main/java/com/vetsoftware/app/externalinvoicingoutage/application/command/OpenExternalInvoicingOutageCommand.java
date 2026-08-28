package com.vetsoftware.app.externalinvoicingoutage.application.command;

import com.vetsoftware.app.externalinvoicingoutage.domain.CauseParty;
import java.time.LocalDateTime;

/**
 * <strong>Sin {@code companyId}, y aqui no es la regla de siempre: es que no
 * existe la columna.</strong> Una caida es un hecho de la plataforma —la sufren
 * varias clinicas a la vez y su causante es el mismo para todas—; el reparto
 * por clinica va en la puente, con su propio caso de uso.
 *
 * @param startedAt
 *            cuando empezo la interrupcion. Es un dato observado, no el reloj
 *            de quien abre la ficha: la caida casi siempre se detecta despues
 *            de haber empezado
 * @param causeParty
 *            quien la causo. <b>Separa un incidente de un incumplimiento</b>, y
 *            ademas decide la unicidad: solo puede haber una caida abierta por
 *            causante
 * @param affectedCompanyCount
 *            primera estimacion del alcance. No se deduce de la puente porque
 *            al abrir todavia no hay filas hijas, y esa estimacion es justo lo
 *            que hace util la ficha antes de que nadie haya repartido nada
 * @param externalIncidentRef
 *            el radicado del proveedor. Opcional al abrir —rara vez lo dan en
 *            caliente— y es lo que traslada la responsabilidad con nombre y
 *            numero
 */
public record OpenExternalInvoicingOutageCommand(LocalDateTime startedAt, CauseParty causeParty,
        String summary, int affectedCompanyCount, String externalIncidentRef) {
}
