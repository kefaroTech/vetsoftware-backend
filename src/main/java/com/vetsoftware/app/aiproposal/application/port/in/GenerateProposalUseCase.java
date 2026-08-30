package com.vetsoftware.app.aiproposal.application.port.in;

import com.vetsoftware.app.aiproposal.application.command.GenerateProposalCommand;
import com.vetsoftware.app.aiproposal.application.dto.ProposalViewDto;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;

/**
 * Genera la propuesta inicial a partir del texto libre de un prospecto que
 * todavia no tiene cuenta.
 *
 * <p>
 * <strong>Abrir esta ruta al mundo son CUATRO cosas</strong>, y las cuatro se
 * comprueban solas:
 *
 * <ol>
 * <li>{@code new Route(HttpMethod.POST, "/assistant/proposal")} en
 * {@code PublicRoutes.BUSINESS}, con patron <strong>literal</strong> -jamas
 * {@code /assistant/**}, que abriria de paso todo lo que acabe colgando del
 * prefijo-. Sin esta linea el {@code AuthFilter} corta con 401 antes de que
 * nadie mire la anotacion;</li>
 * <li>esta anotacion. Sin ella {@code PUERTOS_AUTORIZADOS} rompe el build;</li>
 * <li>la ruta escrita tambien en el {@code containsExactlyInAnyOrder} de
 * {@code PublicRoutesTest}, que afirma el inventario completo a proposito: es
 * la que todo el mundo olvida;</li>
 * <li>su {@code RouteLimit} en {@code LoginRateLimitFilter}, porque es un
 * {@code POST} anonimo. {@code POST_SIN_LIMITE_JUSTIFICADO} esta vacio: no hay
 * ni un POST publico perdonado.</li>
 * </ol>
 *
 * <p>
 * &#9940; <strong>La implementacion no puede llevar
 * {@code @Transactional}.</strong> Escribe la cabecera y el turno
 * {@code PENDING} y <em>commitea</em>; invoca al modelo fuera de toda
 * transaccion; y abre una segunda transaccion para cerrar el turno.
 * {@code SIN_IO_EXTERNO_EN_TRANSACCION} sigue la cadena de llamadas completa y
 * veta por prefijo los paquetes de los SDK de IA: esconder la invocacion tres
 * metodos mas abajo no la despista.
 */
@NoAuthorizationRequired(reason = "Lo llama un prospecto anonimo desde la landing comercial: quien pide la propuesta todavia no tiene cuenta -de hecho el objetivo del endpoint es que la cree- y exigir token haria imposible el embudo entero. No lee ni escribe dato alguno de ninguna empresa: ai_proposals no tiene company_id y no puede tenerlo, y el catalogo que consulta es global de plataforma. Lo que produce no es vinculante -emitir una cotizacion formal exige quote.request y ese endpoint no es publico- y su unica frontera de autorizacion es el public_token de 43 caracteres que devuelve, que jamas viaja en un segmento de ruta. El abuso lo acota su RouteLimit por IP mas el tope de gasto diario del modelo.")
public interface GenerateProposalUseCase {

    ProposalViewDto generate(GenerateProposalCommand command);
}
