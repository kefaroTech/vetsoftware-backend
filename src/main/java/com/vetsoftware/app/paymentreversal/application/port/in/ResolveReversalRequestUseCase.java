package com.vetsoftware.app.paymentreversal.application.port.in;

import com.vetsoftware.app.paymentreversal.application.command.ResolveReversalRequestCommand;
import com.vetsoftware.app.paymentreversal.application.dto.PaymentReversalRequestDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ResolveReversalRequestUseCase {

    /**
     * Cierra el expediente con uno de los cuatro desenlaces.
     *
     * <p>
     * Solo plataforma: aceptar una reversion saca dinero, y sacar dinero exige
     * firma. La devolucion enlazada —{@code resulting_refund_id}— se valida acotada
     * por empresa antes de guardarse.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PaymentReversalRequestDto execute(ResolveReversalRequestCommand command);
}
