package com.vetsoftware.app.quote.application.port.out;

import java.util.Optional;

/**
 * Traduce el <strong>token publico</strong> de una propuesta del asistente a su
 * <strong>id</strong>, que es lo que la cotizacion guarda en
 * {@code quotes.ai_proposal_id}.
 *
 * <p>
 * <strong>Por que la cotizacion guarda el id y no el token.</strong> El token
 * es el secreto de la URL y la unica frontera de seguridad de
 * {@code aiproposal}; copiarlo a {@code quotes} lo multiplicaria por dos y lo
 * sacaria del control de acceso que lo protege. Mismo criterio, y misma razon,
 * que {@code legal_document_acceptances.subject_ref}.
 *
 * <p>
 * <strong>Para que sirve la columna.</strong> El embudo real es propuesta
 * &rarr; cotizacion &rarr; registro &rarr; empresa, y sin este eslabon un
 * abandono entre la propuesta y la cotizacion y uno entre la cotizacion y el
 * registro son el mismo numero. El changeset que creo la columna razona ademas
 * por que se puso antes de hacer falta: la relacion no se puede reconstruir a
 * posteriori, porque el unico vinculo —que el prospecto venia de un enlace— no
 * se escribe en ninguna otra parte.
 *
 * <p>
 * <strong>Vacio si el token no existe, y el alta de la cotizacion
 * sigue.</strong> La FK de la columna va {@code ON DELETE SET NULL} justamente
 * porque la propuesta se purga por retencion y la cotizacion no: la respuesta
 * honesta a «de que propuesta salio esta oferta» pasa a ser «ya no se sabe», no
 * un id que apunta a nada.
 */
public interface ProposalReferencePort {

    Optional<Long> findIdByPublicToken(String publicToken);
}
