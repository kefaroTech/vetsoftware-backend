package com.vetsoftware.app.subscriptionpayment.application.usecase;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ListAllSubscriptionPaymentsUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentRepository;
import com.vetsoftware.app.subscriptionpayment.application.query.ListAllSubscriptionPaymentsQuery;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Observed(name = "subscription.payment.list.all")
@Service
public class ListAllSubscriptionPaymentsService implements ListAllSubscriptionPaymentsUseCase {

    private final SubscriptionPaymentRepository repository;
    private final Clock clock;

    public ListAllSubscriptionPaymentsService(SubscriptionPaymentRepository repository,
            Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public PageResult<SubscriptionPaymentDto> listAll(ListAllSubscriptionPaymentsQuery query) {
        LocalDateTime pendingThreshold = pendingThreshold(query, clock);
        SubscriptionPaymentStatus effectiveStatus = pendingThreshold != null
                ? SubscriptionPaymentStatus.PENDING
                : query.status();
        return repository
                .findAllFiltered(query.companyId(), effectiveStatus, query.receivedFrom(),
                        query.receivedTo(), pendingThreshold, query.page(), query.pageSize())
                .map(SubscriptionPaymentDto::from);
    }

    static LocalDateTime pendingThreshold(ListAllSubscriptionPaymentsQuery query, Clock clock) {
        return query.pendingOlderThanMinutes() == null
                ? null
                : LocalDateTime.now(clock).minusMinutes(query.pendingOlderThanMinutes());
    }
}
