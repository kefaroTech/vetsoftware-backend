package com.vetsoftware.app.paymentreversal.application.port.in;

import com.vetsoftware.app.paymentreversal.application.command.AcknowledgeReversalRequestCommand;
import com.vetsoftware.app.paymentreversal.application.dto.PaymentReversalRequestDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface AcknowledgeReversalRequestUseCase {

    /**
     * Deja constancia del acuse entregado al cliente.
     *
     * <p>
     * Solo plataforma: es un acto propio frente al reclamante. La constancia es
     * <strong>obligatoria</strong> pero posterior al nacimiento de la fila, y lo
     * que vigila que llegue es el barrido de
     * {@link ListExpiringReversalRequestsUseCase}, no un {@code NOT NULL} que
     * impediria abrir el expediente.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PaymentReversalRequestDto execute(AcknowledgeReversalRequestCommand command);
}
