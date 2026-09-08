package com.vetsoftware.app.quote.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.quote.application.port.out.ImmediatePaidLineOpeningPort;
import com.vetsoftware.app.subscription.application.command.AddSubscriptionItemCommand;
import com.vetsoftware.app.subscription.application.command.RequestedSubscriptionItemCommand;
import com.vetsoftware.app.subscription.application.port.in.AddSubscriptionItemUseCase;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Abre la línea de pago inmediata reutilizando el otrosí de
 * {@link AddSubscriptionItemUseCase} — mismo cableado de escalada a
 * {@code SYSTEM} que {@code AcceptedQuoteSubscriptionProvisioner}: quien llega
 * hasta aquí ya pasó por {@code PurchaseModulesUseCase}, que revalidó la
 * empresa y el rol ADMIN antes de invocar este adaptador.
 */
@Component
public class ImmediatePaidLineOpeningAdapter implements ImmediatePaidLineOpeningPort {

    private static final String REASON = "Compra de módulo desde el escaparate de autoservicio";

    private final AddSubscriptionItemUseCase addSubscriptionItemUseCase;
    private final SystemAuthRunner systemAuthRunner;

    public ImmediatePaidLineOpeningAdapter(AddSubscriptionItemUseCase addSubscriptionItemUseCase,
            SystemAuthRunner systemAuthRunner) {
        this.addSubscriptionItemUseCase = addSubscriptionItemUseCase;
        this.systemAuthRunner = systemAuthRunner;
    }

    @Override
    public void openNow(Long companyId, Long subscriptionId, Long catalogItemId, Long employeeId,
            Long quoteId, String clientRequestId, LocalDate today) {
        systemAuthRunner.run(() -> addSubscriptionItemUseCase
                .execute(new AddSubscriptionItemCommand(subscriptionId, companyId, clientRequestId,
                        today, REASON, employeeId, null, quoteId,
                        new RequestedSubscriptionItemCommand(catalogItemId, 1, today, null))));
    }
}
