package com.vetsoftware.app.dunning.application.port.in;

import com.vetsoftware.app.dunning.application.dto.DunningBatchResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Reclama y reevalua un lote de facturas vencidas <b>de todos los tenants</b>.
 *
 * <p>
 * <b>Cerrado a {@code hasRole('SYSTEM')} a secas, y existe como puerto
 * precisamente para poder decirlo.</b> Hasta que este puerto existio, el
 * barrido vivia como metodo publico de un {@code @Service} que no implementaba
 * nada: su unica proteccion era que el unico llamador —el job programado— se
 * autenticase antes con {@code SystemAuthRunner}. Eso es un hecho del arbol, no
 * una propiedad del codigo: inyectar el bean en un controller nuevo bastaba
 * para entregar las facturas vencidas de todas las clinicas, y ninguna de las
 * reglas de arquitectura lo miraba porque todas parten de
 * {@code ..application.port.in..} o de los puertos que la clase implementa.
 *
 * <p>
 * Es literalmente el supuesto de {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}: un
 * barrido que no filtra por empresa devuelve filas de todas, asi que solo lo
 * puede servir un principal cross-tenant. Los dos barridos hermanos
 * ({@code ListBillingDocumentsAwaitingExternalUseCase},
 * {@code ListOverdueBillingDocumentsUseCase}) ya lo llevaban escrito; este
 * hacia lo mismo y no llevaba nada.
 */
public interface ProcessDunningBatchUseCase {

    /**
     * Procesa el lote que sigue al cursor. Devuelve cuantas facturas se reevaluaron
     * y el ultimo id visto, que es el cursor de la siguiente vuelta.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    DunningBatchResult processBatchAfter(long afterId, int batchSize);
}
