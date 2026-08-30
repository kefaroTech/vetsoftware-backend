package com.vetsoftware.app.aiproposal.application.port.in;

import com.vetsoftware.app.aiproposal.application.dto.ProposalViewDto;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;

/**
 * Relee una propuesta ya generada. Es lo que abre el enlace del correo.
 *
 * <p>
 * &#9940; <strong>El token entra por {@code ?token=}, no por
 * {@code /{token}}.</strong> Es el precedente de las tres rutas anonimas que ya
 * existen -{@code /auth/reset-password/validate},
 * {@code /platform/access-request/validate} y
 * {@code /platform/invitation/validate}, las tres con {@code @RequestParam
 * String token}-, y no es casualidad: {@code getRequestURI()} no incluye la
 * cadena de consulta, y es {@code getRequestURI()} lo que
 * {@code RequestLoggingContextFilter} escribe en el contexto de log de toda
 * peticion. Con el token en el path acabaria intacto en CloudWatch y en Loki
 * -ningun patron del redactor casa con 43 caracteres de base64url- y ademas en
 * el {@code Referer} que el navegador manda a terceros.
 *
 * <p>
 * &#9888; <strong>Lo que se pierde y se acepta por escrito:</strong> con el
 * token fuera del path, {@code http.path} deja de distinguir "leer la propuesta
 * A" de "leer la propuesta B". Es exactamente lo que se busca; la correlacion
 * se hace con el campo estructurado {@code proposal.id}, que es un
 * identificador interno y no un secreto.
 *
 * <p>
 * <strong>Y como la ruta es literal, no la toca la trampa del
 * {@code RouteLimit} con variable</strong>: {@code routeLimit()} la casa con
 * {@code equals} y {@code LoginRateLimitFilterTest.rutaConcreta} -que no
 * expande {@code &#123;var&#125;}- no tiene nada que expandir aqui.
 */
@NoAuthorizationRequired(reason = "Es la lectura de una propuesta anonima por parte del prospecto que la pidio: no hay cuenta que exigir -el enlace se abre desde el correo, muchas veces en otro dispositivo- y la unica credencial es el public_token de 43 caracteres de SecureRandom, que llega como parametro de consulta para que no acabe en el contexto de log ni en el Referer. No devuelve dato de ninguna empresa ni el veredicto de ninguna linea: solo las aceptadas y un entero con cuantas se descartaron.")
public interface GetProposalUseCase {

    ProposalViewDto get(String publicToken);
}
