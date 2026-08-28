package com.vetsoftware.app.taxreturn.application.port.in;

import com.vetsoftware.app.taxreturn.application.command.FileTaxReturnCommand;
import com.vetsoftware.app.taxreturn.application.dto.TaxReturnDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FileTaxReturnUseCase {

    /**
     * Presenta la declaracion: fecha, firmante, radicado, copia y fecha de firmeza,
     * las cinco a la vez.
     *
     * <p>
     * Es el momento en que {@code firmezaUntil} empieza a existir, y con el la
     * ventana de conservacion de todos los soportes que esta declaracion sostiene.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    TaxReturnDto execute(FileTaxReturnCommand command);
}
