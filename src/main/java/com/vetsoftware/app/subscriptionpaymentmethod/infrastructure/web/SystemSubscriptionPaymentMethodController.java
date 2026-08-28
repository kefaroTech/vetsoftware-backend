package com.vetsoftware.app.subscriptionpaymentmethod.infrastructure.web;

import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.subscriptionpaymentmethod.application.command.ExpireSubscriptionPaymentMethodCommand;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.in.ExpireSubscriptionPaymentMethodUseCase;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.in.ListAllSubscriptionPaymentMethodsUseCase;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.in.ListExpiringPaymentMethodsUseCase;
import com.vetsoftware.app.subscriptionpaymentmethod.infrastructure.web.response.SubscriptionPaymentMethodResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * El lado de plataforma del parque de medios de pago: el barrido de tarjetas
 * por vencer, la constatacion de que una vencio, y la consulta cross-tenant de
 * la consola.
 *
 * <p>
 * Los tres puertos que consume estan cerrados a {@code hasRole('SYSTEM')} a
 * secas. Ninguno de ellos filtra por empresa —ese es justo su cometido—, y por
 * eso ninguno puede estar abierto por permiso: lo que el tenant necesita ver de
 * sus propias tarjetas sale por {@link SubscriptionPaymentMethodController}.
 */
@RestController
@RequestMapping("/system/subscription-payment-methods")
public class SystemSubscriptionPaymentMethodController {

    private final ListExpiringPaymentMethodsUseCase listExpiringUseCase;
    private final ListAllSubscriptionPaymentMethodsUseCase listAllUseCase;
    private final ExpireSubscriptionPaymentMethodUseCase expireUseCase;

    public SystemSubscriptionPaymentMethodController(
            ListExpiringPaymentMethodsUseCase listExpiringUseCase,
            ListAllSubscriptionPaymentMethodsUseCase listAllUseCase,
            ExpireSubscriptionPaymentMethodUseCase expireUseCase) {
        this.listExpiringUseCase = listExpiringUseCase;
        this.listAllUseCase = listAllUseCase;
        this.expireUseCase = expireUseCase;
    }

    /**
     * Tarjetas que caducan antes de {@code before}, de todas las clinicas. Es el
     * insumo del aviso previo: avisar antes de que se venza, en vez de que el
     * cliente lo descubra con el cobro rechazado.
     */
    @GetMapping("/expiring")
    public PageResponse<SubscriptionPaymentMethodResponse> listExpiring(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate before,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listExpiringUseCase.listExpiring(before, page, pageSize),
                SubscriptionPaymentMethodResponse::from);
    }

    @GetMapping
    public PageResponse<SubscriptionPaymentMethodResponse> listAll(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listAllUseCase.listAll(companyId, page, pageSize),
                SubscriptionPaymentMethodResponse::from);
    }

    /**
     * Constata que el mandato de una tarjeta caduco. La empresa viaja como
     * parametro porque quien llama es la plataforma —no tiene una propia— y la
     * carga por id va acotada por ella de todos modos.
     */
    @PatchMapping("/{id}/expiration")
    public SubscriptionPaymentMethodResponse expire(@PathVariable Long id,
            @RequestParam Long companyId) {
        return SubscriptionPaymentMethodResponse.from(
                expireUseCase.execute(new ExpireSubscriptionPaymentMethodCommand(id, companyId)));
    }
}
