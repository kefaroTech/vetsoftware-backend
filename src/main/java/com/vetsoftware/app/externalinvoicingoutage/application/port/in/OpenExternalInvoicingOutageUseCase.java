package com.vetsoftware.app.externalinvoicingoutage.application.port.in;

import com.vetsoftware.app.externalinvoicingoutage.application.command.OpenExternalInvoicingOutageCommand;
import com.vetsoftware.app.externalinvoicingoutage.application.dto.ExternalInvoicingOutageDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface OpenExternalInvoicingOutageUseCase {

    /**
     * Abre la ficha de una caida de la emision fiscal.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas, y la ausencia de un camino de
     * tenant es la decision.</strong> La caida es un hecho de la plataforma: la
     * sufren varias clinicas a la vez, su causante es el mismo para todas y
     * declarar quien la causo —{@code EXTERNAL_ISSUER} u {@code OWN}— es lo que
     * separa un incidente de un incumplimiento propio. Abrirlo por permiso dejaria
     * que un tenant escribiera sobre una ficha que leen todos los demas y, peor,
     * que declarara ajena una caida nuestra.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    ExternalInvoicingOutageDto execute(OpenExternalInvoicingOutageCommand command);
}
