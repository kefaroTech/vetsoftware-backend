package com.vetsoftware.app.paymentrefund.application.port.in;

import com.vetsoftware.app.paymentrefund.application.command.RegisterPaymentRefundCommand;
import com.vetsoftware.app.paymentrefund.application.dto.PaymentRefundDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface RegisterPaymentRefundUseCase {

    /**
     * Registra una devolucion de dinero ya cobrado. Es <strong>idempotente</strong>
     * (R13): la misma llave de cliente devuelve la devolucion que ya se creo en vez
     * de devolver el dinero dos veces.
     *
     * <p>
     * <strong>Cerrado a {@code hasRole('SYSTEM')} a secas, y la ausencia de un
     * camino de tenant es la decision, no un olvido.</strong> El documento maestro
     * reparte este bloque -«Cobro y saldos»- como <em>escribe plataforma, leen
     * ambos</em>: el cliente ve sus devoluciones y no las hace. Sacar dinero de la
     * caja de Lumbre es tesoreria de la plataforma, y ademas exige firma
     * ({@code authorized_by_system_user_id} es {@code NOT NULL}); una firma que
     * pudiera poner el propio beneficiario no es una firma.
     *
     * <p>
     * <strong>Este parrafo existe para el dia que llegue la peticion.</strong> Una
     * clinica pide poder registrar ella misma su devolucion; quien la atienda no
     * lee el changelog, lee este puerto. Abrir el camino de tenant no es sembrar un
     * permiso: es cambiar quien autoriza una salida de caja, y obliga a decidir
     * antes quien firma cuando el que pide y el que aprueba son el mismo.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PaymentRefundDto execute(RegisterPaymentRefundCommand command);
}
