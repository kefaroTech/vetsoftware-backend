package com.vetsoftware.app.aiproposal.application.command;

/**
 * La peticion de supresion de un titular, por correo.
 *
 * <p>
 * <strong>Sin {@code companyId}, y no es un olvido</strong>: una propuesta es
 * anonima por construccion y no pertenece a ninguna empresa. Quien puede pedir
 * esto es {@code SYSTEM}, que es quien responde por el producto ante la SIC.
 *
 * @param executedBySystemUserId
 *            &#9940; <strong>Lo pone el controller desde la sesion
 *            ({@code authz.currentSystemUserId()}), nunca el cuerpo de la
 *            peticion.</strong> {@code SuppressProposalDataRequest} no tiene ni
 *            tendra este campo: un rastro de auditoria que escribe el auditado
 *            no es un rastro de auditoria, es un formulario. Y es obligatorio
 *            -no {@code currentSystemUserIdOrNull}- porque una evidencia que no
 *            puede nombrar a quien atendio la peticion no sirve para lo unico
 *            que se le va a pedir.
 */
public record SuppressProposalDataCommand(String contactEmail, Long executedBySystemUserId) {

    public SuppressProposalDataCommand {
        if (contactEmail == null || contactEmail.isBlank()) {
            throw new IllegalArgumentException("contactEmail is required");
        }
        if (executedBySystemUserId == null) {
            throw new IllegalArgumentException("executedBySystemUserId is required");
        }
    }
}
