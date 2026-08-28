package com.vetsoftware.app.withholdingcertificate.application.port.in;

import com.vetsoftware.app.withholdingcertificate.application.command.AttachSubstituteEvidenceCommand;
import com.vetsoftware.app.withholdingcertificate.application.dto.WithholdingCertificateDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface AttachSubstituteEvidenceUseCase {

    /**
     * Adjunta el comprobante de pago cuando el cliente no expidio el certificado.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas</strong>, por lo mismo que
     * {@link ReceiveWithholdingCertificateUseCase}: el command senala una fila por
     * {@code id} y no transporta empresa (BE-COV,
     * {@code OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM}).
     *
     * <p>
     * Y ademas por el fondo: decidir que una retencion se acredita con el
     * comprobante de pago en vez de con el certificado es una decision con
     * consecuencias ante la administracion. No es subir un archivo, es afirmar que
     * el obligado incumplio y que se usa el soporte que la ley admite en su lugar.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    WithholdingCertificateDto execute(AttachSubstituteEvidenceCommand command);
}
