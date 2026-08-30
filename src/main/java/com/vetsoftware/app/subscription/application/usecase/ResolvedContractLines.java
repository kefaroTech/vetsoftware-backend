package com.vetsoftware.app.subscription.application.usecase;

import com.vetsoftware.app.subscription.application.command.SubscriptionItemLineCommand;
import java.util.List;

/**
 * Las lineas ya resueltas de un contrato que todavia no se ha firmado, con el
 * actor que queda en la bitacora.
 *
 * <p>
 * Existe porque hay <strong>tres</strong> caminos que producen exactamente esto
 * —el alta pedida desde la consola, el alta desde catalogo publicado y la
 * sustitucion por cotizacion aceptada— y los tres tienen que entregarselo a la
 * misma primitiva. Un tipo comun es lo que impide que el tercero se escriba
 * «parecido» a los dos primeros.
 */
record ResolvedContractLines(String actor, List<SubscriptionItemLineCommand> items) {
}
