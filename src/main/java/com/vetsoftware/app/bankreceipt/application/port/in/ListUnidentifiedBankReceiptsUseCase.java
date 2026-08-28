package com.vetsoftware.app.bankreceipt.application.port.in;

import com.vetsoftware.app.bankreceipt.application.dto.BankReceiptDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * La bandeja de lo no identificado: el trabajo pendiente del mes.
 *
 * <p>
 * Es el caso de uso que da sentido al indice {@code ix_bank_receipts_inbox
 * (status, received_on)}. Sin el, ese indice seria un adorno; con el, la
 * consulta que mas se ejecuta de esta feature lo recorre en el mismo orden en
 * que esta escrito.
 */
public interface ListUnidentifiedBankReceiptsUseCase {

    /**
     * Las entradas que siguen sin dueño, <strong>de la mas antigua a la mas
     * reciente</strong>: una consignacion que lleva tres semanas sin explicar es un
     * cliente que puede estar reclamando, y atender la bandeja por lo ultimo que
     * llego la deja al final para siempre.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas</strong>, igual que el listado
     * completo y por la misma razon: la tabla no tiene empresa.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<BankReceiptDto> listUnidentified(int page, int pageSize);
}
