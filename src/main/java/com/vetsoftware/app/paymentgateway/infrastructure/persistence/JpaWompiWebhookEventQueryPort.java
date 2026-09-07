package com.vetsoftware.app.paymentgateway.infrastructure.persistence;

import com.vetsoftware.app.paymentgateway.application.dto.WompiWebhookEventDto;
import com.vetsoftware.app.paymentgateway.application.port.out.WompiWebhookEventQueryPort;
import com.vetsoftware.app.paymentgateway.application.query.ListWompiWebhookEventsQuery;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class JpaWompiWebhookEventQueryPort implements WompiWebhookEventQueryPort {

    private final WompiWebhookEventJpaRepository jpaRepository;

    public JpaWompiWebhookEventQueryPort(WompiWebhookEventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PageResult<WompiWebhookEventDto> search(ListWompiWebhookEventsQuery query) {
        Sort order = Sort.by(Sort.Direction.DESC, "receivedAt")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        return Pages.result(
                jpaRepository.search(query.reference(), query.companyId(), query.from(), query.to(),
                        Pages.request(query.page(), query.pageSize(), order)),
                JpaWompiWebhookEventQueryPort::toDto);
    }

    private static WompiWebhookEventDto toDto(WompiWebhookEventJpaEntity entity) {
        return new WompiWebhookEventDto(entity.getId(), entity.getGateway(), entity.getEventType(),
                entity.getGatewayReference(), entity.getEventChecksum(),
                entity.getProcessingOutcome(), entity.getReceivedAt(), entity.getProcessedAt(),
                entity.getCreatedDate(), entity.getRawBody());
    }
}
