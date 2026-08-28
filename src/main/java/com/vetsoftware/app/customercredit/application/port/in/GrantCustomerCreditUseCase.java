package com.vetsoftware.app.customercredit.application.port.in;

import com.vetsoftware.app.customercredit.application.command.GrantCustomerCreditCommand;
import com.vetsoftware.app.customercredit.application.dto.CustomerCreditEntryDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface GrantCustomerCreditUseCase {

    /**
     * Abona saldo a favor a una empresa, abriendo un lote.
     *
     * <p>
     * <strong>Cerrado a {@code hasRole('SYSTEM')} a secas, y la ausencia de un
     * camino de tenant es la decision, no un olvido.</strong> El bloque <em>Cobro y
     * saldos</em> del modelo reparte esta tabla como «escribe plataforma, leen
     * ambos»: el cliente ve su saldo, no se lo concede. Un abono es tesoreria de la
     * plataforma —sale de un pago en exceso, de una nota credito o de una baja con
     * periodo pagado por delante—, y dejar que el beneficiario se lo escriba a si
     * mismo convierte el saldo a favor en un campo de texto.
     *
     * <p>
     * Es <strong>idempotente</strong>: la misma llave de cliente devuelve el
     * asiento que ya se escribio en vez de abonar dos veces.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    CustomerCreditEntryDto execute(GrantCustomerCreditCommand command);
}
