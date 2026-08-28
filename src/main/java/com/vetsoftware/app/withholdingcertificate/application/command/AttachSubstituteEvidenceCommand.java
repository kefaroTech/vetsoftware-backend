package com.vetsoftware.app.withholdingcertificate.application.command;

import com.vetsoftware.app.withholdingcertificate.domain.SubstituteEvidenceKind;

/**
 * El cliente no expidio el certificado y la retencion se acredita con el
 * comprobante de pago, que es el unico sustituto que la ley admite.
 *
 * <p>
 * Sin {@code companyId} por la misma razon que
 * {@link ReceiveWithholdingCertificateCommand}, y con la misma consecuencia: su
 * puerto esta cerrado a {@code hasRole('SYSTEM')} a secas.
 */
public record AttachSubstituteEvidenceCommand(Long id, SubstituteEvidenceKind evidenceKind,
        String evidenceRef) {
}
