package com.vetsoftware.app.aiproposal.application.command;

import com.vetsoftware.app.aiproposal.domain.ProposalBillingCycle;
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
 * @param billingCycle
 *            &#9940; <strong>El defecto a {@code MONTHLY} vive AQUI y no en el
 *            request.</strong> {@code GenerateProposalRequest} lo declara
 *            opcional para no romper a los clientes ya desplegados, pero si el
 *            defecto se aplicara alli, cualquier otro llamante del caso de uso
 *            -otro controller, un job, un test- podria construir el comando con
 *            {@code null} y llegar hasta {@code AiProposal.create}, que lo
 *            exige y reventaria con un {@code IllegalArgumentException} a mitad
 *            de la escritura. Aqui el nulo es imposible aguas abajo.
 *            <p>
 *            <strong>Es un dato de la propuesta, no metadato de
 *            transporte</strong> -a diferencia de {@code idempotencyKey}, que
 *            por eso es cabecera-: cambia el precio, se persiste en
 *            {@code ai_proposals.billing_cycle} y es lo que
 *            {@code ProposalReader.catalogo} vuelve a leer para que un
 *            refinamiento cotice contra la misma escalera de
 *            {@code catalog_prices} que la generacion.
 */
public record GenerateProposalCommand(String contactEmail, String description,
        String idempotencyKey, List<LegalAcceptanceCommand> acceptances, String acceptedIpHash,
        String userAgentHash, ProposalBillingCycle billingCycle) {

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
        billingCycle = billingCycle == null ? ProposalBillingCycle.MONTHLY : billingCycle;
    }
}
