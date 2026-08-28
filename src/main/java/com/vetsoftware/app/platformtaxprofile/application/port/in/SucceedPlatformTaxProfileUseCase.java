package com.vetsoftware.app.platformtaxprofile.application.port.in;

import com.vetsoftware.app.platformtaxprofile.application.command.SucceedPlatformTaxProfileCommand;
import com.vetsoftware.app.platformtaxprofile.application.dto.PlatformTaxProfileDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Cambia la identidad fiscal de VetSoftware cerrando la vigente y abriendo su
 * sucesora, en una sola transaccion.
 *
 * <p>
 * <strong>Este puerto es el que ocupa el sitio del {@code update} que no
 * existe.</strong> Quien busque «como se corrige el NIT de VetSoftware» acabara
 * aqui, y lo que tiene que leer es que no se corrige: se sucede. La ficha vieja
 * se queda intacta porque las facturas ya emitidas tienen que seguir diciendo
 * con que razon social se emitieron.
 */
public interface SucceedPlatformTaxProfileUseCase {

    /**
     * Devuelve la ficha <strong>nueva</strong>, no la cerrada: es la que rige a
     * partir de {@code effectiveFrom} y la que la consola tiene que pintar.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PlatformTaxProfileDto execute(SucceedPlatformTaxProfileCommand command);
}
