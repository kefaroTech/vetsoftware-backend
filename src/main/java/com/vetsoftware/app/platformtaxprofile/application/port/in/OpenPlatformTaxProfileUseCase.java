package com.vetsoftware.app.platformtaxprofile.application.port.in;

import com.vetsoftware.app.platformtaxprofile.application.command.OpenPlatformTaxProfileCommand;
import com.vetsoftware.app.platformtaxprofile.application.dto.PlatformTaxProfileDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface OpenPlatformTaxProfileUseCase {

    /**
     * Abre la primera identidad fiscal de Lumbre.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas, y la ausencia de un camino de
     * tenant es la decision.</strong> Esta es la razon social y el NIT de la
     * plataforma: el dato que se imprime en la factura de <em>todos</em> los
     * clientes. Abrirlo por permiso dejaria que un tenant reescribiera a nombre de
     * quien se le factura a la plataforma entera. No hay caso de uso hermano
     * acotado por empresa porque no hay empresa que acotar: la tabla es global y no
     * tiene {@code company_id}.
     *
     * <p>
     * <strong>Toda la feature esta cerrada a SYSTEM por el mismo motivo</strong>,
     * incluidas las lecturas — lo exige ademas
     * {@code GATE_COHERENTE_EN_FEATURE_DE_SYSTEM}: en una feature cerrada a SYSTEM,
     * una authority suelta es un endpoint que se abre sembrando un permiso.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PlatformTaxProfileDto execute(OpenPlatformTaxProfileCommand command);
}
