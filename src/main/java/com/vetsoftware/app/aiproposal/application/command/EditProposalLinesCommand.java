package com.vetsoftware.app.aiproposal.application.command;

import java.util.List;

/**
 * La edicion manual, que es <strong>soberana</strong>: lo que el cliente quita
 * no vuelve solo, y lo que anade se conserva aunque el modelo no lo proponga
 * (plan S8.3).
 *
 * <p>
 * El token va en el cuerpo por el mismo motivo que en el refinamiento.
 */
public record EditProposalLinesCommand(String publicToken, List<String> addedCodes,
        List<String> removedCodes, Long expectedVersion) {

    public EditProposalLinesCommand {
        if (publicToken == null || publicToken.isBlank())
            throw new IllegalArgumentException("publicToken is required");
        addedCodes = addedCodes == null ? List.of() : List.copyOf(addedCodes);
        removedCodes = removedCodes == null ? List.of() : List.copyOf(removedCodes);
        if (addedCodes.isEmpty() && removedCodes.isEmpty())
            throw new IllegalArgumentException("an edit must add or remove at least one line");
    }
}
