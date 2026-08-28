package com.vetsoftware.app.legaldocumentversion.application.port.in;

import com.vetsoftware.app.legaldocumentversion.application.command.PublishLegalDocumentVersionCommand;
import com.vetsoftware.app.legaldocumentversion.application.dto.LegalDocumentVersionDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Publica una version nueva y sucede a la vigente.
 *
 * <p>
 * <strong>No existe un {@code UpdateLegalDocumentVersionUseCase}</strong>, y
 * esa ausencia es la decision: el disparador {@code trg_ldv_bu_immutable}
 * rechaza editar el contenido o la huella, asi que un caso de uso de edicion
 * seria una promesa que el motor no puede cumplir. Suceder es la operacion;
 * editar no existe.
 */
public interface PublishLegalDocumentVersionUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    LegalDocumentVersionDto execute(PublishLegalDocumentVersionCommand command);
}
