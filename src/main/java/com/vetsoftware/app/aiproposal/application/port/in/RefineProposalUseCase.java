package com.vetsoftware.app.aiproposal.application.port.in;

import com.vetsoftware.app.aiproposal.application.command.RefineProposalCommand;
import com.vetsoftware.app.aiproposal.application.dto.ProposalViewDto;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;

/**
 * Anade texto a una propuesta que ya existe y la recalcula.
 *
 * <p>
 * <strong>Los turnos son ACUMULATIVOS</strong> (plan S7.2.1): al modelo se le
 * manda la descripcion original y todos los anadidos, en orden y con su rotulo.
 * Mandar solo el ultimo es la regresion silenciosa que borraria las siete
 * lineas del primer turno cuando alguien escriba "tambien hacemos peluqueria"
 * -el modelo devolveria {@code GROOMING} y nada mas, con razon-, y el copy que
 * hay encima del campo promete literalmente lo contrario.
 *
 * <p>
 * <strong>Tope de tres refinamientos</strong>, y al cuarto se devuelve 200 con
 * la propuesta intacta y {@code recalculated = false}. Nunca un 400: la
 * interfaz lo leeria como una averia.
 */
@NoAuthorizationRequired(reason = "Es la continuacion de la misma conversacion anonima que abrio POST /assistant/proposal: el prospecto sigue sin cuenta y acredita que la propuesta es suya con el public_token de 43 caracteres que recibio, que viaja en el cuerpo y nunca en la ruta. Sin company_id por construccion, sin dato de ninguna empresa, y con el mismo tope de gasto y el mismo RouteLimit por IP que el endpoint inicial. El tope de tres refinamientos por propuesta lo comprueba el caso de uso contra los turnos de modelo ya escritos.")
public interface RefineProposalUseCase {

    ProposalViewDto refine(RefineProposalCommand command);
}
