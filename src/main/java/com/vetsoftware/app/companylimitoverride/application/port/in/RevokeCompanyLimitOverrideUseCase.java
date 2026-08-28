package com.vetsoftware.app.companylimitoverride.application.port.in;

import com.vetsoftware.app.companylimitoverride.application.command.RevokeCompanyLimitOverrideCommand;
import com.vetsoftware.app.companylimitoverride.application.dto.CompanyLimitOverrideDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Cierra una excepción negociada.
 *
 * <p>
 * No la borra: escribe quién la quitó, cuándo y por qué, y le pone fecha de
 * fin. Así «¿qué techo tenía el 14 de marzo?» sigue teniendo respuesta, y el
 * eje queda libre para negociar otro pacto ese mismo día.
 *
 * <p>
 * Autorización: {@code hasRole('SYSTEM')} a secas, por lo mismo que la
 * concesión.
 */
public interface RevokeCompanyLimitOverrideUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    CompanyLimitOverrideDto execute(RevokeCompanyLimitOverrideCommand command);
}
