package com.vetsoftware.app.aiproposal.application.port.out;

import com.vetsoftware.app.aiproposal.domain.AiProposal;
import com.vetsoftware.app.aiproposal.domain.ProposalLine;
import com.vetsoftware.app.aiproposal.domain.ProposalTurn;
import java.util.List;
import java.util.Optional;

/**
 * El puerto de salida de la rodaja: cabecera, turnos y lineas.
 *
 * <p>
 * ⛔ <strong>Ni un metodo con {@code companyId} ni con {@code Company} en el
 * nombre</strong>, ni siquiera uno que nadie use. No es estilo: esa es la senal
 * exacta con la que {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} decide si un
 * puerto "sabe filtrar por empresa", y basta uno para que todo {@code find...}
 * suyo que devuelva varias filas pase a exigir {@code hasRole('SYSTEM')} a
 * secas -que es justo lo que un prospecto anonimo no puede tener-. Un prospecto
 * no es una empresa, y esta rodaja no tiene ninguna de la que tirar.
 *
 * <p>
 * <strong>Los tres agregados en un solo puerto, a proposito</strong>: turno y
 * linea no tienen vida fuera de su propuesta -las dos FK van
 * {@code ON DELETE RESTRICT} contra ella- y partirlos en tres puertos
 * multiplicaria por tres los adaptadores sin separar nada.
 */
public interface AiProposalRepository {

    AiProposal save(AiProposal proposal);

    Optional<AiProposal> findById(Long id);

    /**
     * El camino real de lectura: la propuesta se direcciona por el token, no por el
     * id.
     */
    Optional<AiProposal> findByPublicToken(String publicToken);

    /**
     * La busqueda de idempotencia, <strong>acotada al solicitante</strong>.
     *
     * <p>
     * &#9940; Buscar solo por la clave convierte una cabecera que elige el cliente
     * en una consulta de lectura sobre las propuestas ajenas: quien reenvie una
     * clave vista -o acierte una, que con un UUID es improbable pero con un cliente
     * que use un contador no lo es- se llevaria con 200 el texto libre, el correo y
     * las lineas de otro prospecto. Es la misma fuga que el {@code public_token}
     * existe para impedir, entrando por otra puerta. Por eso
     * {@code uq_ai_proposals_idempotency} va sobre
     * {@code (contact_email_hash, idempotency_key)} y esta firma recibe los dos.
     *
     * <p>
     * <strong>Una clave repetida por otro correo no encuentra nada y sigue su
     * camino normal</strong>, que es el comportamiento correcto: dos prospectos que
     * colisionen en un UUID no tienen por que enterarse el uno del otro. Y no se
     * responde 409, que distinguiria "esa clave esta usada" de "no lo esta" y seria
     * un oraculo de enumeracion.
     *
     * <p>
     * El correo llega ya en minusculas porque la columna generada del unico es
     * {@code UNHEX(SHA2(LOWER(contact_email),256))}: con el correo sin normalizar
     * este {@code SELECT} no encontraria la fila con la que el {@code INSERT} si va
     * a chocar.
     */
    Optional<AiProposal> findByIdempotency(String contactEmail, String idempotencyKey);

    ProposalTurn saveTurn(ProposalTurn turn);

    Optional<ProposalTurn> findTurnById(Long turnId);

    List<ProposalTurn> findTurnsByProposalId(Long proposalId);

    /**
     * Las lineas de un turno se escriben en bloque, que es como las produce el
     * motor.
     */
    List<ProposalLine> saveLines(List<ProposalLine> lines);

    List<ProposalLine> findLinesByTurnId(Long turnId);

    void delete(Long id);
}
