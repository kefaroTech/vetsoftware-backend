package com.vetsoftware.app.externalinvoicereconciliation.application.port.in;

import com.vetsoftware.app.externalinvoicereconciliation.application.dto.ExternalInvoiceReconciliationDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * <strong>La consulta que de verdad importa</strong>, y por eso tiene caso de
 * uso propio en vez de ser un filtro mas del barrido general.
 *
 * <p>
 * No es la de las diferencias: es la de los documentos de cobro <em>sin factura
 * externa</em>. Eso es dinero devengado que NADIE facturo, y hoy no lo ve
 * nadie. Los otros tres estados nacen de comparar dos numeros y saltan solos en
 * cualquier listado de descuadres; {@code MISSING_EXTERNAL} no produce ninguna
 * diferencia que llame la atencion, asi que si no se pregunta por el
 * explicitamente no aparece en ningun sitio.
 *
 * <p>
 * Se ordena por antiguedad y sirve {@code ix_eir_pending (status,
 * created_date)}, que existe justo para esto: lo que lleva mas dias sin
 * facturar es lo primero que hay que mirar.
 */
public interface ListMissingExternalInvoicesUseCase {

    /**
     * <strong>{@code hasRole('SYSTEM')} a secas.</strong> Barre todas las empresas
     * por definicion -la pregunta es «que se me quedo sin facturar», no «que se le
     * quedo sin facturar a esta clinica»-, que es el caso literal de
     * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<ExternalInvoiceReconciliationDto> listMissing(int page, int pageSize);
}
