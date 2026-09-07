package com.vetsoftware.app.paymentgateway.application.usecase;

import com.vetsoftware.app.paymentgateway.application.dto.WompiWebhookEventDto;
import com.vetsoftware.app.paymentgateway.application.port.in.ListWompiWebhookEventsUseCase;
import com.vetsoftware.app.paymentgateway.application.port.out.WompiWebhookEventQueryPort;
import com.vetsoftware.app.paymentgateway.application.query.ListWompiWebhookEventsQuery;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "payment.gateway.wompi.webhook.events.list")
@Service
public class ListWompiWebhookEventsService implements ListWompiWebhookEventsUseCase {

    private final WompiWebhookEventQueryPort queryPort;

    public ListWompiWebhookEventsService(WompiWebhookEventQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    @Override
    public PageResult<WompiWebhookEventDto> search(ListWompiWebhookEventsQuery query) {
        return queryPort.search(query);
    }
}
