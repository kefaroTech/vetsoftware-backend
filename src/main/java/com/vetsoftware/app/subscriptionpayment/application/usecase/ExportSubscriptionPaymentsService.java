package com.vetsoftware.app.subscriptionpayment.application.usecase;

import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ExportSubscriptionPaymentsUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentRepository;
import com.vetsoftware.app.subscriptionpayment.application.query.ListAllSubscriptionPaymentsQuery;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "subscription.payment.export")
@Service
public class ExportSubscriptionPaymentsService implements ExportSubscriptionPaymentsUseCase {

    private final SubscriptionPaymentRepository repository;
    private final Clock clock;

    public ExportSubscriptionPaymentsService(SubscriptionPaymentRepository repository,
            Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public List<SubscriptionPaymentDto> export(ListAllSubscriptionPaymentsQuery query) {
        LocalDateTime pendingThreshold = ListAllSubscriptionPaymentsService.pendingThreshold(query,
                clock);
        SubscriptionPaymentStatus effectiveStatus = pendingThreshold != null
                ? SubscriptionPaymentStatus.PENDING
                : query.status();
        return repository
                .findAllFilteredForExport(query.companyId(), effectiveStatus, query.receivedFrom(),
                        query.receivedTo(), pendingThreshold)
                .stream().map(SubscriptionPaymentDto::from).toList();
    }
}
