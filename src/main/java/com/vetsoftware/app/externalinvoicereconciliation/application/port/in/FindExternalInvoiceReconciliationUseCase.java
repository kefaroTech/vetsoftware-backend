package com.vetsoftware.app.externalinvoicereconciliation.application.port.in;

import com.vetsoftware.app.externalinvoicereconciliation.application.dto.ExternalInvoiceReconciliationDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * La lectura por id de una conciliacion, y <strong>el sitio donde esta escrita
 * la decision de autorizacion de toda la rodaja</strong>. Los otros cinco
 * puertos remiten aqui.
 */
public interface FindExternalInvoiceReconciliationUseCase {

    /**
     * <strong>{@code hasRole('SYSTEM')} a secas, tambien para leer, y la ausencia
     * del camino de tenant es LA DECISION, no un olvido.</strong>
     *
     * <p>
     * En este bloque no existe controller de tenant. Ni uno. Toda la rodaja se
     * sirve desde {@code /system/external-invoice-reconciliations}, y ningun puerto
     * de entrada -ni siquiera esta lectura por id- lleva la alternativa
     * {@code or (hasAuthority(...) and @authz.isMyCompany(...))} que si llevan los
     * bloques que el documento maestro reparte como «escribe plataforma, leen
     * ambos».
     *
     * <p>
     * <strong>El motivo, textual:</strong> la conciliacion es el cuadre entre
     * Lumbre y su facturador externo, y ensenarsela al cliente es ensenarle el
     * margen y los datos de terceros. Aqui viven el total que Lumbre calculo frente
     * al que emitio el tercero, el numero y el rango de la resolucion de numeracion
     * ajena, y la nota interna con la que alguien de la plataforma explico un
     * descuadre. Nada de eso es informacion del cliente: es la contabilidad de
     * quien le cobra.
     *
     * <p>
     * <strong>Este parrafo existe para el dia que llegue la peticion de
     * abrirla.</strong> Una clinica pedira ver «su» conciliacion, y quien atienda
     * esa peticion no va a leer el changelog: va a leer este puerto. Abrir el
     * camino de tenant no es anadir un {@code hasAuthority}; es decidir <em>que
     * subconjunto</em> de estos campos puede ver quien paga la factura, y eso
     * empieza por un DTO distinto -sin {@code computedTax} enfrentado a
     * {@code externalTax}, sin la resolucion del tercero y sin
     * {@code resolutionNote}-, no por relajar esta expresion.
     *
     * <p>
     * La regla {@code OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM} (BE-COV) exige
     * exactamente esto para un puerto que recibe un {@code Long} y no recibe
     * {@code companyId}.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    ExternalInvoiceReconciliationDto findById(Long id);
}
