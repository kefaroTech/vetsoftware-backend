package com.vetsoftware.app.aiproposal.application.command;

import java.util.List;
import java.util.Locale;

/**
 * Lo que llega de la pantalla 1.
 *
 * <p>
 * &#9940; <strong>Sin {@code companyId}, y no puede tenerlo</strong>: un
 * prospecto no es una empresa. Las cuatro reglas de BE-COV no se activan aqui
 * -el puerto va {@code @NoAuthorizationRequired}- pero el motivo de fondo es el
 * mismo y conviene dejarlo escrito: no hay ninguna empresa de la que tirar.
 *
 * @param contactEmail
 *            ya normalizado a minusculas y sin espacios, y se normaliza
 *            <strong>aqui</strong> y no en el controller porque de eso depende
 *            que la busqueda de idempotencia case con
 *            {@code uq_ai_proposals_idempotency}: ese unico va sobre la columna
 *            generada {@code UNHEX(SHA2(LOWER(contact_email),256))}, asi que
 *            con el correo en mayusculas el {@code SELECT} no encuentra la fila
 *            con la que el {@code INSERT} si va a chocar
 * @param idempotencyKey
 *            el UUID que el front genera al montar la pantalla. Puede ser nulo
 *            -un cliente que no lo mande no pierde el servicio, solo la
 *            proteccion contra el doble clic-
 */
public record GenerateProposalCommand(String contactEmail, String description,
        String idempotencyKey, List<LegalAcceptanceCommand> acceptances, String acceptedIpHash,
        String userAgentHash) {

    public GenerateProposalCommand {
        if (contactEmail == null || contactEmail.isBlank())
            throw new IllegalArgumentException("contactEmail is required");
        contactEmail = contactEmail.trim().toLowerCase(Locale.ROOT);
        if (contactEmail.length() > 320)
            throw new IllegalArgumentException("contactEmail must be 320 chars or less");
        if (description == null || description.isBlank())
            throw new IllegalArgumentException("description is required");
        if (idempotencyKey != null && idempotencyKey.length() != 36)
            throw new IllegalArgumentException("idempotencyKey must be a 36-char UUID");
        acceptances = acceptances == null ? List.of() : List.copyOf(acceptances);
    }
}
