package com.vetsoftware.app.entitlement.application.port.in;

import com.vetsoftware.app.entitlement.application.command.MarkCapacityUsageReconciledCommand;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Sella el consumo de un contador (R-ENT-13): deja escrito cuando se comprobo
 * por ultima vez que cuadra con las filas reales.
 *
 * <h2>Autorizacion: {@code hasRole('SYSTEM')} a secas</h2>
 *
 * <p>
 * Por el mismo motivo por el que lo esta la correccion de consumo: el sello es
 * la afirmacion «este contador esta comprobado», y quien puede escribirla sobre
 * su propio contador puede declararlo sano sin haber contado nada. La empresa
 * viaja en el command porque quien lo escribe es plataforma, actuando sobre un
 * tenant que no es el suyo.
 *
 * <p>
 * <strong>Sella y nada mas.</strong> No toca el techo, no toca el consumo y no
 * corrige: si el recuento encontro desvio, lo que se escribe es un hecho
 * {@code USAGE_RECONCILED} y este sello <em>no</em> se pone. Sellar un contador
 * que se sabe desviado dejaria el indicador de salud diciendo «sano» justo
 * sobre el dato que acaba de demostrarse malo.
 *
 * @return {@code true} si habia contador que sellar
 */
public interface MarkCapacityUsageReconciledUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    boolean execute(MarkCapacityUsageReconciledCommand command);
}
