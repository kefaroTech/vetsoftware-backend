package com.vetsoftware.app.customercredit.application.port.in;

import com.vetsoftware.app.customercredit.application.command.ExpireCustomerCreditCommand;
import com.vetsoftware.app.customercredit.application.dto.CustomerCreditEntryDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ExpireCustomerCreditUseCase {

    /**
     * Caduca el remanente de los lotes de una empresa cuya fecha ya paso,
     * escribiendo un asiento {@code EXPIRATION} por lote.
     *
     * <p>
     * <strong>Cerrado a {@code hasRole('SYSTEM')} a secas.</strong> Caducar saldo
     * es quitarle dinero al cliente: es la escritura menos discutible de todo el
     * bloque, y el propio beneficiario no puede ser quien la dispare ni quien la
     * evite.
     *
     * <p>
     * Idempotente por dia: la llave de idempotencia de cada asiento lleva la fecha
     * valor y el lote, asi que repetir el barrido el mismo dia no caduca nada dos
     * veces.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    List<CustomerCreditEntryDto> execute(ExpireCustomerCreditCommand command);
}
