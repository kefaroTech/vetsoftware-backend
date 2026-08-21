package com.vetsoftware.app.openaccount.application.port.in;

import com.vetsoftware.app.openaccount.application.command.MarkOpenAccountReversedCommand;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;

/**
 * Puerto de entrada del reverso de cartera. Es la unica via por la que se
 * estampa {@code reversed} sobre una cuenta: obliga a pasar por
 * {@code OpenAccount.markReversed(...)}, que es donde vive la regla (solo una
 * cuenta CLOSE se reversa, y una segunda vez no reescribe la fecha).
 *
 * <p>
 * Antes el reverso lo escribia a mano un adaptador de la feature
 * electronicdocument, que habia copiado la mitad de la regla —la idempotencia—
 * y omitido la otra —la guarda de estado—. Dos verdades sobre lo mismo, y la
 * del dominio sin ejecutar en produccion (incidencia #124).
 *
 * <p>
 * Internal-only: nunca se mapea a un endpoint REST. Lo consume un adaptador de
 * electronicdocument, igual que {@code ClosedAccountEmissionPort} conecta el
 * cierre con la emision en sentido contrario.
 */
@NoAuthorizationRequired(reason = "Efecto de la validacion DIAN de una nota credito: llega por el webhook firmado del proveedor, sin JWT de empleado del que sacar la empresa; por eso viaja en el command y acota la lectura.")
public interface MarkOpenAccountReversedUseCase {

    /**
     * No-op si la cuenta no existe en esa empresa o si ya estaba reversada. Lanza
     * {@code IllegalStateException} si la cuenta no esta CLOSE: reversar algo que
     * nunca se facturo es un error de programacion del llamador, no un caso
     * tolerable.
     */
    void execute(MarkOpenAccountReversedCommand command);
}
