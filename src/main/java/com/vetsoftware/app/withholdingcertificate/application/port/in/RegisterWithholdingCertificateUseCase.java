package com.vetsoftware.app.withholdingcertificate.application.port.in;

import com.vetsoftware.app.withholdingcertificate.application.command.RegisterWithholdingCertificateCommand;
import com.vetsoftware.app.withholdingcertificate.application.dto.WithholdingCertificateDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface RegisterWithholdingCertificateUseCase {

    /**
     * Abre la expectativa de un certificado de retencion.
     *
     * <p>
     * <strong>Cerrado a {@code hasRole('SYSTEM')} a secas, y la ausencia de un
     * camino de tenant es la decision.</strong> El certificado lo expide un tercero
     * -el cliente que retuvo- y lo registra tesoreria de la plataforma cuando
     * concilia la cartera; el cliente lo <em>lee</em>, porque la retencion es plata
     * suya, y no lo escribe. Dejar que la clinica se registre a si misma un
     * certificado seria dejar que declare por su cuenta cuanto le retuvieron.
     *
     * <p>
     * <strong>Este parrafo existe para el dia que llegue la peticion.</strong> Una
     * clinica pide subir ella misma el certificado que le mandaron; quien la
     * atienda no lee el changelog, lee este puerto. Abrir el camino de tenant no es
     * sembrar un permiso: es cambiar quien afirma un hecho fiscal, y obliga a
     * decidir antes que pasa cuando lo afirmado no coincide con el papel.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    WithholdingCertificateDto execute(RegisterWithholdingCertificateCommand command);
}
