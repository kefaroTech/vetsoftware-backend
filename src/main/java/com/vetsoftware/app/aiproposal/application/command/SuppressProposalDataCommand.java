package com.vetsoftware.app.aiproposal.application.command;

/**
 * La peticion de supresion de un titular, por correo.
 *
 * <p>
 * <strong>Sin {@code companyId}, y no es un olvido</strong>: una propuesta es
 * anonima por construccion y no pertenece a ninguna empresa. Quien puede pedir
 * esto es {@code SYSTEM}, que es quien responde por el producto ante la SIC.
 */
public record SuppressProposalDataCommand(String contactEmail) {

    public SuppressProposalDataCommand {
        if (contactEmail == null || contactEmail.isBlank()) {
            throw new IllegalArgumentException("contactEmail is required");
        }
    }
}
