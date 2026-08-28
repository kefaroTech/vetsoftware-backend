package com.vetsoftware.app.paymentreversal.application.port.in;

import com.vetsoftware.app.paymentreversal.application.command.OpposeReversalRequestCommand;
import com.vetsoftware.app.paymentreversal.application.dto.PaymentReversalRequestDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface OpposeReversalRequestUseCase {

    /**
     * Registra la oposicion de la plataforma, con su motivo tasado y su constancia.
     *
     * <p>
     * Solo plataforma, y de forma evidente: es <em>el lado propio del
     * expediente</em>. Oponerse exige constancia, y los tres datos van juntos o no
     * va ninguno.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PaymentReversalRequestDto execute(OpposeReversalRequestCommand command);
}
