package com.vetsoftware.app.electronicdocument.application.port.out;

/**
 * Puerto de salida hacia la cartera (open account): marca una cuenta como
 * reversada cuando la nota credito que la corrige es VALIDADA por la DIAN.
 *
 * <p>
 * <b>La regla no vive aqui, y por eso el adapter no la reimplementa</b>
 * (incidencia #124). Quien decide si una cuenta se puede reversar —solo una
 * CLOSE— y que una segunda pasada no reescriba la fecha es
 * {@code OpenAccount.markReversed(...)}, en el dominio de {@code openaccount}.
 * El adapter de este puerto solo traslada la peticion a ese dominio a traves de
 * {@code MarkOpenAccountReversedUseCase}. Antes la escribia a mano un adaptador
 * de persistencia que habia copiado la mitad de la regla —la idempotencia— y
 * omitido la otra —la guarda de estado—: dos verdades sobre lo mismo, y la del
 * dominio sin ejecutarse nunca en produccion.
 *
 * <p>
 * <b>El {@code companyId} no es decorativo</b>: acota la lectura de la cuenta,
 * asi que una cuenta de otro tenant no se encuentra y por tanto no se escribe.
 * Es defensa en profundidad — hoy el {@code openAccountId} se copia de la
 * factura original ya validada contra la empresa, no llega del cliente.
 */
public interface AccountReversalPort {
    void markReversed(Long openAccountId, Long companyId);
}
