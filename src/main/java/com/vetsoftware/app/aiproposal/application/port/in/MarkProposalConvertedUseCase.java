package com.vetsoftware.app.aiproposal.application.port.in;

import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * <strong>La propuesta acabo en cliente.</strong> Es la unica via por la que
 * {@code ai_proposals.status} llega a {@code CONVERTED}.
 *
 * <p>
 * <strong>Recibe el token publico y devuelve el id, y ese cambio de moneda es
 * el punto.</strong> El token es el secreto de la URL y la unica frontera de
 * seguridad de esta rodaja ({@code ProposalToken}); el id es lo que se puede
 * copiar a otra tabla sin multiplicar el secreto ni sacarlo del control de
 * acceso que lo protege. Es el mismo criterio con el que
 * {@code legal_document_acceptances} guarda el id de la propuesta en
 * {@code subject_ref} y nunca su token.
 *
 * <p>
 * <strong>Un token desconocido devuelve vacio, no lanza.</strong> Quien llama
 * es el alta de una empresa, y una atribucion de embudo <em>jamas</em> puede
 * tumbar un registro: el token puede venir de un enlace viejo cuya propuesta ya
 * se llevo la purga de retencion —que es una obligacion legal y corre sola—, y
 * el prospecto no tiene forma de saberlo ni de arreglarlo. Se pierde la
 * atribucion, que es un dato de analitica, en vez del cliente.
 *
 * <p>
 * <strong>Es idempotente.</strong> Marcar como convertida una propuesta que ya
 * lo estaba devuelve su id sin escribir nada.
 */
public interface MarkProposalConvertedUseCase {

    /**
     * @return el id de la propuesta, o vacio si el token no corresponde a ninguna
     *         viva
     */
    @PreAuthorize("hasRole('SYSTEM')")
    Optional<Long> execute(String publicToken);
}
