package com.vetsoftware.app.smmlvvalue.application.port.in;

import com.vetsoftware.app.smmlvvalue.application.command.ChangeSmmlvStatusCommand;
import com.vetsoftware.app.smmlvvalue.application.dto.SmmlvValueDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * La unica mutacion del bloque, y la razon de que esta tabla lleve columna de
 * concurrencia: la suspension judicial se anota sobre la fila que ya existe.
 */
public interface ChangeSmmlvStatusUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    SmmlvValueDto execute(ChangeSmmlvStatusCommand command);
}
