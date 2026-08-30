package com.vetsoftware.app.aiproposal.application.port.out;

import com.vetsoftware.app.aiproposal.application.dto.ProposalLinkEmail;

/**
 * Manda al prospecto el enlace de su propuesta.
 *
 * <p>
 * <strong>Solo al prospecto.</strong> No hay aviso a ventas y no es un olvido:
 * el lead queda en {@code ai_proposals} para quien lo quiera consultar, y un
 * correo automatico a un buzon interno por cada peticion anonima es un canal
 * que cualquiera puede llenar desde fuera sin cuenta.
 *
 * <p>
 * <strong>Nunca lanza.</strong> Lo invoca un {@code afterCommit}, con la
 * transaccion ya confirmada: una excepcion ahi se propaga al llamante y
 * convierte una propuesta correctamente guardada en un 500.
 */
public interface ProposalLinkEmailSender {

    void send(ProposalLinkEmail email);
}
