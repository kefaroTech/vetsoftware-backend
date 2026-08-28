package com.vetsoftware.app.subscriptionbilling.application.port.in;

import com.vetsoftware.app.subscriptionbilling.application.command.AccrueOverageChargeCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.SubscriptionChargeDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Devenga el cargo por consumo <b>por encima del cupo contratado</b>.
 *
 * <h2>Por qué es un puerto aparte de
 * {@link CreateSubscriptionChargeUseCase}</h2>
 *
 * <p>
 * <b>Porque el gate de aquél lo haría inútil.</b>
 * {@code CreateSubscriptionChargeUseCase} está cerrado a
 * {@code hasRole('SYSTEM')} a secas, y con razón: lo expone un endpoint y un
 * empleado del tenant no puede escribir en su propio devengado. Pero el
 * excedente <b>lo dispara el consumo</b>, y el consumo lo hace el empleado:
 * {@code AdjustCompanyCapacityUsageUseCase} está abierto a
 * {@code @authz.isMyCompany}. Encaminar el excedente por el puerto de
 * plataforma dejaría al tenant exactamente igual de bloqueado que antes —solo
 * que con un 403 en vez de un 409—, es decir, no habría arreglado nada.
 *
 * <p>
 * <b>Y la salida no es ensanchar aquel gate</b>, que convertiría «devengar»
 * —elegir importe, tipo y periodo— en una capacidad del tenant. La salida es
 * este puerto: mismo destino, superficie mínima. El command fija
 * {@code ChargeType.OVERAGE} y no lo deja elegir, y el servicio comprueba
 * contra el contrato que el excedente estaba permitido y a qué precio: un
 * empleado no puede devengarse un cargo arbitrario por aquí.
 *
 * <h2>El gate, y su precedente escrito</h2>
 *
 * <p>
 * {@code hasRole('SYSTEM') or @authz.isMyCompany(#command.companyId)} es
 * literalmente el de {@code RecordLimitEventUseCase}, y por el mismo motivo
 * escrito allí: <b>esto es un efecto del sistema, no una capacidad
 * concedible</b>. No lo expone ningún controller —búsquese: no hay endpoint que
 * llame aquí—; lo llama {@code OverageChargeAdapter} bajo el principal del
 * empleado que acaba de pasarse del cupo, como consecuencia de una operación
 * que ese empleado ya tenía permiso para intentar. Exigirle además una
 * {@code hasAuthority(...)} sería un segundo candado en la misma puerta y, como
 * ya documenta el changeset 370, no habría dónde sembrarla sin dejar el
 * excedente sin cobrar para todo empleado que no sea administrador.
 *
 * <p>
 * El nombre {@code #command.companyId} es load-bearing: el SpEL resuelve por
 * nombre de parámetro y, si dejan de coincidir, evalúa a {@code null} en
 * silencio y la comprobación falla siempre.
 */
public interface AccrueOverageChargeUseCase {

    @PreAuthorize("hasRole('SYSTEM') or @authz.isMyCompany(#command.companyId)")
    SubscriptionChargeDto execute(AccrueOverageChargeCommand command);
}
