package com.vetsoftware.app.paymentgateway.application.port.out;

import com.vetsoftware.app.paymentgateway.application.dto.WompiWebhookEventDto;
import com.vetsoftware.app.paymentgateway.application.query.ListWompiWebhookEventsQuery;
import com.vetsoftware.app.shared.pagination.PageResult;

public interface WompiWebhookEventQueryPort {

    PageResult<WompiWebhookEventDto> search(ListWompiWebhookEventsQuery query);
}
