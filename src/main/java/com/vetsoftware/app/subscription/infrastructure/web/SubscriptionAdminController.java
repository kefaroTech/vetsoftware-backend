package com.vetsoftware.app.subscription.infrastructure.web;

import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import com.vetsoftware.app.subscription.application.dto.SubscriptionItemOverlapDto;
import com.vetsoftware.app.subscription.application.port.in.FindOverlappingSubscriptionItemsUseCase;
import com.vetsoftware.app.subscription.application.port.in.ListAllSubscriptionsUseCase;
import com.vetsoftware.app.subscription.infrastructure.web.response.SubscriptionItemOverlapResponse;
import com.vetsoftware.app.subscription.infrastructure.web.response.SubscriptionResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * La consola de plataforma. Sus dos endpoints barren <strong>todas</strong> las
 * clinicas sin filtrar por empresa, asi que sus puertos estan cerrados a
 * {@code hasRole('SYSTEM')} a secas (BE-29). Lo equivalente para el tenant vive
 * en {@code SubscriptionController} y siempre lleva {@code companyId}.
 */
@RestController
@RequestMapping("/platform-subscriptions")
public class SubscriptionAdminController {

    private final ListAllSubscriptionsUseCase listAllUseCase;
    private final FindOverlappingSubscriptionItemsUseCase findOverlapsUseCase;

    public SubscriptionAdminController(ListAllSubscriptionsUseCase listAllUseCase,
            FindOverlappingSubscriptionItemsUseCase findOverlapsUseCase) {
        this.listAllUseCase = listAllUseCase;
        this.findOverlapsUseCase = findOverlapsUseCase;
    }

    @GetMapping
    public PageResponse<SubscriptionResponse> listAll(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listAllUseCase.listAll(page, pageSize), this::toResponse);
    }

    /**
     * La vigilancia R7. <strong>Lista vacia = sano.</strong> Cada elemento es un
     * par de tramos del mismo articulo que se pisan, y en el tramo comun ese modulo
     * se esta facturando dos veces.
     *
     * <p>
     * Conviene correrla a diario y ademas justo despues de cada despliegue que
     * toque las altas y bajas de linea, que es el momento en que se rompe.
     */
    @GetMapping("/item-overlaps")
    public List<SubscriptionItemOverlapResponse> findOverlaps() {
        return findOverlapsUseCase.findAllOverlaps().stream().map(this::toResponse).toList();
    }

    private SubscriptionResponse toResponse(SubscriptionDto dto) {
        return new SubscriptionResponse(dto.id(), dto.subscriptionNumber(), dto.companyId(),
                dto.quoteId(), dto.priceListId(), dto.billingCycle(), dto.status(), dto.current(),
                dto.startDate(), dto.trialEndDate(), dto.currentPeriodStart(),
                dto.currentPeriodEnd(), dto.nextBillingDate(), dto.commitmentEndDate(),
                dto.graceDays(), dto.pastDueSince(), dto.autoRenew(), dto.cancelRequestedAt(),
                dto.cancelEffectiveDate(), dto.cancelReason(), dto.createdDate(), dto.enabled());
    }

    private SubscriptionItemOverlapResponse toResponse(SubscriptionItemOverlapDto dto) {
        return new SubscriptionItemOverlapResponse(dto.companyId(), dto.subscriptionId(),
                dto.catalogItemId(), dto.itemCode(), dto.firstItemId(), dto.firstFrom(),
                dto.firstTo(), dto.secondItemId(), dto.secondFrom(), dto.secondTo());
    }
}
