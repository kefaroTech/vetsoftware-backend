package com.vetsoftware.app.taxreturn.application.port.in;

import com.vetsoftware.app.taxreturn.application.command.CorrectTaxReturnCommand;
import com.vetsoftware.app.taxreturn.application.dto.TaxReturnDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CorrectTaxReturnUseCase {

    /**
     * Abre la correccion de una declaracion presentada y marca la anterior como
     * corregida, <b>en la misma transaccion</b>.
     *
     * <p>
     * <strong>El orden importa y no es negociable.</strong> Mientras la anterior
     * siga en {@code FILED}, su {@code current_return_marker} vale el supuesto
     * completo y {@code uq_tax_returns_current} impide insertar la nueva. Hacer las
     * dos cosas en transacciones separadas dejaria una ventana en la que el periodo
     * no tiene ninguna declaracion vigente.
     *
     * <p>
     * Devuelve la <em>correccion</em>, que es el borrador nuevo.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    TaxReturnDto execute(CorrectTaxReturnCommand command);
}
