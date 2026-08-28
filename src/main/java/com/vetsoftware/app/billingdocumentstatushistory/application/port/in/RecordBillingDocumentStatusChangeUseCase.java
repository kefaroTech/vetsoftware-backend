package com.vetsoftware.app.billingdocumentstatushistory.application.port.in;

import com.vetsoftware.app.billingdocumentstatushistory.application.command.RecordBillingDocumentStatusChangeCommand;
import com.vetsoftware.app.billingdocumentstatushistory.application.dto.BillingDocumentStatusHistoryDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Apunta un cambio de estado de un documento de cobro.
 *
 * <p>
 * <strong>Es el unico caso de uso de escritura de la feature y solo
 * inserta.</strong> No hay hermano que edite ni que borre: la tabla solo se
 * agrega, asi que un fotograma equivocado se corrige apuntando el movimiento
 * inverso, nunca reescribiendo el anterior.
 */
public interface RecordBillingDocumentStatusChangeUseCase {

    /**
     * <strong>{@code hasRole('SYSTEM')} a secas, y la ausencia de rama por permiso
     * es la decision.</strong> Quien escribe la bitacora de estados decide que se
     * puede probar en una disputa, y la tabla es una de las seis que el modelo
     * declara irreemplazables: un {@code billingDocumentStatusHistory.create}
     * sembrado al tenant dejaria al administrador de una clinica apuntando sobre su
     * propia factura la transicion que le conviene. No seria fuga entre empresas
     * —el servicio comprueba que el documento sea suyo— y por eso es peor: la fila
     * es legitima, esta en su sitio, y falsea el expediente sin dejar rastro.
     *
     * <p>
     * <strong>Por eso el {@code @PostMapping} no vive en el controller de
     * cliente.</strong> Lo sirve
     * {@code SystemBillingDocumentStatusHistoryController}, que elige la empresa
     * como {@code @RequestParam} porque un principal SYSTEM no tiene empresa
     * propia. La lectura si se le deja al cliente: leer la pelicula de su propio
     * documento es exactamente lo que la tabla existe para permitir.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    BillingDocumentStatusHistoryDto execute(RecordBillingDocumentStatusChangeCommand command);
}
