package com.vetsoftware.app.subscription.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.entitlement.application.command.InitializeCompanyEntitlementsCommand;
import com.vetsoftware.app.entitlement.application.port.in.InitializeCompanyEntitlementsUseCase;
import com.vetsoftware.app.infrastructure.logging.MdcKeys;
import com.vetsoftware.app.subscription.application.dto.SubscriptionChangedEvent;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionAuditPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionChangedPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionEntitlementMetrics;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionEntitlementMetrics.Trigger;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Cierra el lazo del modelo: cuando el contrato cambia, los permisos se
 * recalculan (R11).
 *
 * <p>
 * <b>Es la pieza que da sentido a todo lo demas.</b> Sin ella se puede dar de
 * baja un modulo y el cliente lo sigue usando, o contratarlo y no verlo
 * aparecer — exactamente el agujero del sistema viejo que este rediseño existe
 * para cerrar, y que la especificacion describe como <i>«bajar de plan hoy no
 * le quita el acceso a nadie»</i>.
 *
 * <p>
 * <b>Dentro de la misma transaccion, y no se difiere.</b> El servicio destino
 * es {@code @Transactional(REQUIRED)} a proposito, asi que se une a la del
 * cambio de contrato: si el recalculo falla, el cambio tampoco ocurre, y no
 * existe ningun estado intermedio en el que el cliente haya pagado por algo que
 * no puede usar. Aqui <b>no aplica</b> la regla de efectos externos: no sale
 * nada del sistema, es una escritura en la misma base. Diferirlo a
 * {@code afterCommit} o marcarlo {@code @Async} abriria justo la ventana que la
 * transaccion cierra.
 *
 * <p>
 * <b>Por que el puerto interno y no
 * {@code RecalculateCompanyEntitlementsUseCase}.</b> El gateado es hoy
 * {@code @PreAuthorize("hasRole('SYSTEM')")} a secas, y ese es exactamente el
 * problema: este adaptador corre <b>bajo el principal de quien disparo el
 * cambio de contrato</b>, que puede ser el administrador de la clinica
 * modificando su propio plan. Llamar al gateado desde aqui lanzaria
 * {@code AccessDeniedException} <b>dentro de la transaccion</b> y la revertiria
 * entera: el cliente amplia su plan, recibe un 403 y no queda rastro del
 * cambio. La regla que fija el criterio es que <b>una escalada interna nunca
 * puede depender del principal de quien disparo la operacion</b>.
 *
 * <p>
 * <b>Y el interno es defendible sin gate</b> porque <b>deriva en vez de
 * conceder</b>: todo lo que escribe sale del contrato vigente de esa empresa,
 * asi que invocarlo no otorga ni un permiso que el contrato no sostenga. El
 * {@code companyId} tampoco viene de la peticion: sale del contrato que se esta
 * modificando, que el controller ya acoto con {@code authz.currentCompanyId()}.
 * El {@code reason} de {@code InitializeCompanyEntitlementsUseCase} declara por
 * escrito que este camino existe, con sus seis endpoints (#409); antes afirmaba
 * que no lo exponia ningun endpoint, que era falso y dejaba al revisor
 * validando la excepcion contra una afirmacion mentirosa.
 *
 * <p>
 * <b>El nombre {@code Initialize} se queda corto, y es solo el nombre.</b> Ese
 * caso de uso <b>reconstruye desde el contrato vigente</b>: borra los permisos
 * de la empresa y los reinserta. Eso sirve igual para el alta que para
 * cualquier cambio posterior —alta o baja de linea, cambio de cantidad, cambio
 * de estado, cancelacion—, que es justo lo que hace este adaptador con los seis
 * eventos. No hace falta otro puerto para los cambios, y quien lea esto dentro
 * de seis meses no tiene que deducirlo.
 *
 * <p>
 * <b>Y por que va envuelto en {@link SystemAuthRunner}.</b> Desde que el tenant
 * puede cambiar cantidades, dar de baja lineas y cancelar su contrato, este
 * recalculo se ejecuta <b>dentro de la transaccion del cliente y bajo su
 * principal</b>. Un paso interno que exige {@code SYSTEM} y hereda el principal
 * de quien disparo la operacion lanza {@code AccessDeniedException} y revierte
 * la transaccion entera: el peor resultado posible, porque el cliente hizo la
 * operacion, recibe un 403 y no queda rastro de nada.
 *
 * <p>
 * Hoy {@code InitializeCompanyEntitlementsUseCase} esta declarado
 * {@code @NoAuthorizationRequired} y no lanzaria — pero eso es una propiedad
 * <b>del otro slice</b>, a un commit de distancia de cambiar, y el dia que
 * cambie el sintoma seria que las ampliaciones de contrato del tenant empiezan
 * a revertir con 403. Escalar aqui hace que la correccion de este slice no
 * dependa de esa decision ajena. Es el mismo patron de
 * {@code PlatformInitialContractProvisioningAdapter}.
 *
 * <p>
 * <b>Escalar no difiere nada.</b> {@code SystemAuthRunner} solo intercambia la
 * autenticacion del {@code SecurityContextHolder} y la restaura en un
 * {@code finally}: la llamada sigue siendo sincrona y sigue en la misma
 * transaccion, que es justo lo que el parrafo de arriba exige. Y no ensancha lo
 * que el cliente puede conseguir: este caso de uso <b>deriva</b> del contrato
 * vigente de esa misma empresa, asi que ejecutarlo como SYSTEM no concede ni un
 * permiso que el contrato no sostenga.
 */
@Component
public class EntitlementRecalculationAdapter implements SubscriptionChangedPort {

    private final InitializeCompanyEntitlementsUseCase initializeCompanyEntitlementsUseCase;
    private final SystemAuthRunner systemAuthRunner;
    private final SubscriptionEntitlementMetrics metrics;
    private final SubscriptionAuditPort audit;

    public EntitlementRecalculationAdapter(
            InitializeCompanyEntitlementsUseCase initializeCompanyEntitlementsUseCase,
            SystemAuthRunner systemAuthRunner, SubscriptionEntitlementMetrics metrics,
            SubscriptionAuditPort audit) {
        this.initializeCompanyEntitlementsUseCase = initializeCompanyEntitlementsUseCase;
        this.systemAuthRunner = systemAuthRunner;
        this.metrics = metrics;
        this.audit = audit;
    }

    /**
     * <b>Aqui se instrumenta el lazo, y no dentro de entitlements</b> (#606): lo
     * que interesa medir es que un cambio de lo que el cliente PAGA se convierta en
     * un cambio de lo que el cliente PUEDE USAR. Medirlo en el otro slice contaria
     * tambien los recalculos que no nacen de un cambio de contrato y mezclaria dos
     * poblaciones.
     *
     * <p>
     * El fallo se cuenta y se relanza: el recalculo corre dentro de la transaccion
     * del cambio de contrato, asi que la excepcion se lleva por delante tambien el
     * cambio —eso esta bien— pero sin contador nadie se entera de que hubo intento,
     * y el sintoma que llega es «varias clinicas dicen que no pueden entrar» sin
     * ninguna serie que apunte aqui. No se registra ademas un log en esta capa: la
     * excepcion se propaga y se registra donde se maneja, no donde pasa de largo.
     */
    @Override
    public void subscriptionChanged(SubscriptionChangedEvent event) {
        Trigger trigger = currentTrigger();
        try {
            // Se descarta lo que devuelve —son contadores— para no arrastrar a este slice
            // un DTO de aplicacion de otra feature.
            systemAuthRunner.run(() -> initializeCompanyEntitlementsUseCase
                    .execute(new InitializeCompanyEntitlementsCommand(event.companyId())));
        } catch (RuntimeException exception) {
            metrics.recalculationFailed(trigger);
            throw exception;
        }
        metrics.recalculated(trigger);
        audit.entitlementsRecalculated(event.companyId(), trigger.value());
    }

    /**
     * Dos poblaciones con dueno y urgencia distintos, y la unica senal que las
     * separa es si hay un barrido en el MDC: un pico a las tres de la manana es el
     * barrido nocturno haciendo su trabajo; el mismo pico al mediodia son clientes
     * esperando frente a una pantalla. Se lee el MDC y no un parametro porque el
     * disparador es una propiedad del CONTEXTO DE EJECUCION, no del evento: el
     * mismo SubscriptionChangedEvent lo emite un controller y lo emite el barrido.
     */
    private static Trigger currentTrigger() {
        return MDC.get(MdcKeys.JOB_NAME) == null
                ? Trigger.SUBSCRIPTION_CHANGED
                : Trigger.SCHEDULED_SWEEP;
    }
}
