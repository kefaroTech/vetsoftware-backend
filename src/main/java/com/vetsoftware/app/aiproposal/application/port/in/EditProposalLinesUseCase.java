package com.vetsoftware.app.aiproposal.application.port.in;

import com.vetsoftware.app.aiproposal.application.command.EditProposalLinesCommand;
import com.vetsoftware.app.aiproposal.application.dto.ProposalViewDto;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;

/**
 * La edicion manual del carrito: quitar y anadir lineas sin volver a llamar al
 * modelo.
 *
 * <p>
 * <strong>No invoca a nadie</strong>, asi que su turno nace cerrado y no
 * consume ni un token. Es el unico de los cuatro endpoints que escribe sin
 * pagar.
 *
 * <p>
 * &#9940; <strong>La edicion es soberana</strong> (plan S8.3): lo que el
 * cliente quita queda escrito como una linea {@code REMOVED} de origen
 * {@code CUSTOMER}, y el refinamiento siguiente <em>no</em> lo vuelve a anadir
 * aunque el modelo lo proponga. Sin eso, el usuario quita "Facturacion
 * electronica" porque no factura, escribe "tambien hacemos peluqueria", y la
 * factura vuelve; con ocho lineas en un movil no se fija y contrata lo que
 * rechazo.
 *
 * <p>
 * &#9888; Es un {@code PUT} anonimo, es decir una <strong>escritura publica sin
 * sesion</strong>. La invariante {@code toda_ruta_publica_post_esta_limitada}
 * solo recorre los {@code POST}, asi que su limite no lo exige ningun gate: se
 * declara igualmente en {@code LoginRateLimitFilter}, con una rama propia
 * anterior al filtro por metodo.
 */
@NoAuthorizationRequired(reason = "Es la edicion del carrito de una propuesta anonima: el prospecto no tiene cuenta y acredita que la propuesta es suya con el public_token que viaja en el cuerpo. No toca ninguna fila de ninguna empresa -ai_proposals no tiene company_id- ni emite ninguna oferta vinculante: solo reescribe que lineas quiere ver. Su limite por IP se declara en LoginRateLimitFilter aunque sea un PUT y la invariante automatica solo mire los POST.")
public interface EditProposalLinesUseCase {

    ProposalViewDto edit(EditProposalLinesCommand command);
}
