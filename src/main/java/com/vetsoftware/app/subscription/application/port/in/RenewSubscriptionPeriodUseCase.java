package com.vetsoftware.app.subscription.application.port.in;

import com.vetsoftware.app.subscription.application.command.RenewSubscriptionPeriodCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Avanza el periodo facturado del contrato.
 *
 * <p>
 * <b>Este puerto existe porque {@code Subscription.renewPeriod} no tenia un
 * solo llamador en produccion.</b> El metodo estaba escrito, probado en su test
 * de dominio y nunca se ejecutaba: el {@code current_period_start/end} de un
 * contrato se quedaba en el valor con el que nacio, para siempre. Los dos danos
 * que eso causaba no se parecen entre si, y por eso ninguno de los dos apunto
 * nunca a la causa:
 *
 * <ol>
 * <li><b>El contrato deja de facturar despues del primer mes.</b> El barrido
 * vuelve a mirar el mismo periodo, encuentra sus cargos ya sellados y no emite
 * nada.
 * <li><b>El prorrateo de una conversion da cero.</b> La formula mide los dias
 * del cambio contra el periodo en curso; si ese periodo se quedo congelado en
 * el pasado, el tramo afectado no lo cruza, salen cero dias y —hasta que se
 * corrigio junto con este puerto— cero pesos guardados en silencio como si
 * fueran un resultado.
 * </ol>
 *
 * <p>
 * <b>{@code hasRole('SYSTEM')} a secas.</b> Mover el periodo facturado es una
 * operacion del motor de cobro, no del tenant: un cliente que pudiera avanzar
 * su propio periodo se saltaria un mes de factura. Mismo criterio que
 * {@code GenerateBillingDocumentUseCase}, que tambien recibe {@code companyId}
 * en su command y tambien esta cerrado a SYSTEM porque el principal es
 * cross-tenant por diseno.
 */
public interface RenewSubscriptionPeriodUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    SubscriptionDto execute(RenewSubscriptionPeriodCommand command);
}
