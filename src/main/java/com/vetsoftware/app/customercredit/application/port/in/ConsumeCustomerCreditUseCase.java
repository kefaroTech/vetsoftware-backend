package com.vetsoftware.app.customercredit.application.port.in;

import com.vetsoftware.app.customercredit.application.command.ConsumeCustomerCreditCommand;
import com.vetsoftware.app.customercredit.application.dto.CustomerCreditEntryDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ConsumeCustomerCreditUseCase {

    /**
     * Aplica saldo a favor a un documento de cobro, repartiendolo entre los lotes
     * vivos <strong>empezando por el que antes caduca</strong> (D-71).
     *
     * <p>
     * Devuelve <strong>una lista</strong> y no un solo asiento porque un consumo
     * casi nunca es una sola fila: se escribe una por cada lote tocado, cada una
     * anotando de donde salio. Devolver solo la primera escondería la mitad de lo
     * que acaba de pasar.
     *
     * <p>
     * <strong>Cerrado a {@code hasRole('SYSTEM')} a secas.</strong> Mismo criterio
     * que el abono: escribe plataforma. Que el cliente pudiera decidir cuando y
     * contra que documento se gasta su saldo es una escritura sobre la capa del
     * dinero, no una preferencia de la clinica.
     *
     * @throws com.vetsoftware.app.customercredit.domain.InsufficientCustomerCreditException
     *             si no hay saldo suficiente. Lo decide el motor, no una lectura
     *             previa
     */
    @PreAuthorize("hasRole('SYSTEM')")
    List<CustomerCreditEntryDto> execute(ConsumeCustomerCreditCommand command);
}
