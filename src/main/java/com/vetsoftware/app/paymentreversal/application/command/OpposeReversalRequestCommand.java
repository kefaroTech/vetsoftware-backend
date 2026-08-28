package com.vetsoftware.app.paymentreversal.application.command;

import com.vetsoftware.app.paymentreversal.domain.OppositionGround;

/**
 * @param oppositionEvidenceRef
 *            constancia de la oposicion. Sin ella no hay oposicion, solo una
 *            afirmacion propia. La fecha la pone el servidor
 */
public record OpposeReversalRequestCommand(Long id, Long companyId, OppositionGround ground,
        String oppositionEvidenceRef) {
}
