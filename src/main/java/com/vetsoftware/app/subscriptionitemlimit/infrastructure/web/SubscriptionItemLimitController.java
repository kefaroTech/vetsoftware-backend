package com.vetsoftware.app.subscriptionitemlimit.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.subscriptionitemlimit.application.port.in.ListSubscriptionItemLimitsUseCase;
import com.vetsoftware.app.subscriptionitemlimit.infrastructure.web.response.SubscriptionItemLimitResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lo que la clínica ve de sus propios techos: <strong>solo lectura</strong>.
 *
 * <p>
 * Que exista este controller es la mitad del valor del plan con tope. La ficha
 * de construcción reparte este bloque como <em>mixto</em> —escribe plataforma,
 * leen ambos— y la razón está escrita: un tope que solo aparece impreso en la
 * cotización no sirve; tiene que ser visible dentro del producto en todo
 * momento, porque es lo que convierte un portazo en algo que el usuario
 * entiende.
 *
 * <p>
 * <strong>La empresa la pone el backend</strong> con
 * {@code authz.currentCompanyId()}: no viaja en ninguna ruta ni en ningún
 * cuerpo de este controller. El puerto la revalida con
 * {@code @authz.isMyCompany(#companyId)}, que es la defensa en profundidad
 * contra un caller futuro que pase otra.
 *
 * <p>
 * Congelar un techo y propagar una mejora viven en
 * {@link SystemSubscriptionItemLimitController}, cerrado a {@code ROLE_SYSTEM}:
 * el cliente no se congela sus propios cupos, igual que no se emite su propia
 * factura.
 */
@RestController
@RequestMapping("/subscription-item-limits")
public class SubscriptionItemLimitController {

    private final ListSubscriptionItemLimitsUseCase listUseCase;
    private final Authz authz;

    public SubscriptionItemLimitController(ListSubscriptionItemLimitsUseCase listUseCase,
            Authz authz) {
        this.listUseCase = listUseCase;
        this.authz = authz;
    }

    @GetMapping
    public List<SubscriptionItemLimitResponse> listMine() {
        return listUseCase.listByCompanyId(authz.currentCompanyId()).stream()
                .map(SubscriptionItemLimitResponse::from).toList();
    }
}
