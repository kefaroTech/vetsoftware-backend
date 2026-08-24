package com.vetsoftware.app.subscriptionpayment.application.port.in;

import com.vetsoftware.app.subscriptionpayment.application.command.RegisterSubscriptionPaymentCommand;
import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface RegisterSubscriptionPaymentUseCase {

    /**
     * Registra un pago recibido. Es <strong>idempotente</strong> (R13): la misma
     * llave de cliente o el mismo par {@code (gateway, gatewayReference)} devuelven
     * el pago que ya se creo en vez de crear otro.
     *
     * <p>
     * <strong>Cerrado a {@code hasRole('SYSTEM')} a secas, y la ausencia de un
     * camino de tenant es la decision, no un olvido.</strong> Cobrar la suscripcion
     * es tesoreria de la plataforma: igual que un cliente no se emite a si mismo la
     * factura de su proveedor, un empleado de la clinica no registra el cobro de lo
     * que su clinica debe. Es el mismo criterio ya escrito en
     * {@code CreateSubscriptionChargeUseCase}, y la semilla dice lo mismo
     * ({@code 257_seed_subscriptionpayment_dunning_permissions.xml}): de los nueve
     * codigos verificados uno a uno, solo los tres {@code *.read} se siembran para
     * tenants; {@code subscriptionPayment.create} no existe en
     * {@code base_permissions} <em>a proposito</em>.
     *
     * <p>
     * <strong>Este parrafo existe para el dia que llegue la peticion.</strong> Una
     * clinica pide registrar ella misma su transferencia; quien la atienda no lee
     * el changelog, lee este puerto. Antes veia una anotacion muda y podia concluir
     * que bastaba con sembrar el permiso. La decision es esta y esta escrita aqui:
     * abrir el camino de tenant no es un {@code INSERT} en
     * {@code base_permissions}, es cambiar quien cobra — y exige revisar tambien la
     * escalada que documenta {@code JpaDunningSubscriptionPort#changeStatus}, hoy
     * ya resuelta con {@code SystemAuthRunner} para que no reviente el pago si eso
     * pasa.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    SubscriptionPaymentDto execute(RegisterSubscriptionPaymentCommand command);
}
