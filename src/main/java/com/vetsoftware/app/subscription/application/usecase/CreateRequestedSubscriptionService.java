package com.vetsoftware.app.subscription.application.usecase;

import com.vetsoftware.app.subscription.application.command.CreateRequestedSubscriptionCommand;
import com.vetsoftware.app.subscription.application.command.CreateSubscriptionCommand;
import com.vetsoftware.app.subscription.application.command.RequestedSubscriptionItemCommand;
import com.vetsoftware.app.subscription.application.command.SubscriptionItemLineCommand;
import com.vetsoftware.app.subscription.application.dto.PublishedCatalogItem;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import com.vetsoftware.app.subscription.application.dto.SubscriptionItemSnapshot;
import com.vetsoftware.app.subscription.application.dto.SubscriptionQuoteSnapshot;
import com.vetsoftware.app.subscription.application.port.in.CreateRequestedSubscriptionUseCase;
import com.vetsoftware.app.subscription.application.port.out.ResolvedSubscriptionCreationPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionCommercialSnapshotPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionQuoteSnapshotPort;
import com.vetsoftware.app.subscription.domain.ContractPriceTiers;
import com.vetsoftware.app.subscription.domain.ContractTierLine;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resuelve en servidor todos los snapshots antes de firmar un contrato. */
@Observed(name = "subscription.create.requested")
@Service
public class CreateRequestedSubscriptionService implements CreateRequestedSubscriptionUseCase {

    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final ResolvedSubscriptionCreationPort creationPort;
    private final SubscriptionQuoteSnapshotPort quoteSnapshotPort;
    private final SubscriptionCommercialSnapshotPort commercialSnapshotPort;

    public CreateRequestedSubscriptionService(ResolvedSubscriptionCreationPort creationPort,
            SubscriptionQuoteSnapshotPort quoteSnapshotPort,
            SubscriptionCommercialSnapshotPort commercialSnapshotPort) {
        this.creationPort = creationPort;
        this.quoteSnapshotPort = quoteSnapshotPort;
        this.commercialSnapshotPort = commercialSnapshotPort;
    }

    @Override
    @Transactional
    public SubscriptionDto execute(CreateRequestedSubscriptionCommand command) {
        ResolvedRequest resolved = command.quoteId() == null
                ? resolveFromPublishedCatalog(command)
                : resolveFromAcceptedQuote(command);
        return creationPort.create(new CreateSubscriptionCommand(command.companyId(),
                command.quoteId(), command.priceListId(), command.billingCycle(), command.status(),
                command.startDate(), command.trialEndDate(), command.currentPeriodStart(),
                command.currentPeriodEnd(), command.nextBillingDate(), command.commitmentEndDate(),
                command.graceDays(), command.autoRenew(), resolved.actor(), resolved.items()));
    }

    /**
     * De la cotizacion aceptada NO hay que repartir nada: los renglones ya vienen
     * partidos por tramo desde que se emitio la oferta (D-66), y cada uno trae su
     * tramo, su precio y su descuento congelados. Copiarlos uno a uno es
     * precisamente lo que hace que el contrato diga lo mismo que el papel que firmo
     * el cliente.
     */
    private ResolvedRequest resolveFromAcceptedQuote(CreateRequestedSubscriptionCommand command) {
        if (command.items() != null && !command.items().isEmpty())
            throw new IllegalArgumentException("items must be omitted when quoteId is provided");
        SubscriptionQuoteSnapshot quote = quoteSnapshotPort
                .findByIdAndCompanyId(command.quoteId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Quote not found for company: " + command.quoteId()));
        if (!quote.accepted())
            throw new IllegalStateException("Quote must be ACCEPTED: " + quote.id());
        if (!Objects.equals(quote.priceListId(), command.priceListId()))
            throw new IllegalArgumentException("priceListId does not match accepted quote");
        if (quote.billingCycle() != command.billingCycle())
            throw new IllegalArgumentException("billingCycle does not match accepted quote");
        if (quote.items() == null || quote.items().isEmpty())
            throw new IllegalStateException("Accepted quote has no contract lines: " + quote.id());
        return new ResolvedRequest(
                quote.acceptedBy() == null || quote.acceptedBy().isBlank()
                        ? SYSTEM_ACTOR
                        : quote.acceptedBy(),
                quote.items().stream().map(item -> toLine(item, null, null)).toList());
    }

    /**
     * Del catalogo publicado si hay que repartir: la seleccion trae una cantidad y
     * los tramos son acumulativos, asi que un articulo escalonado produce
     * <b>varias</b> lineas de contrato -una por tramo, cada una a su precio-. Trece
     * unidades extra son ocho a 12.000 y cinco a 9.000, no trece al precio del
     * tramo alto.
     */
    private ResolvedRequest resolveFromPublishedCatalog(
            CreateRequestedSubscriptionCommand command) {
        if (command.items() == null || command.items().isEmpty())
            throw new IllegalArgumentException("items are required when quoteId is absent");
        List<SubscriptionItemLineCommand> lines = new ArrayList<>();
        for (RequestedSubscriptionItemCommand requested : command.items()) {
            if (requested == null || requested.catalogItemId() == null)
                throw new IllegalArgumentException("catalogItemId is required");
            int quantity = requested.quantity() == null ? 1 : requested.quantity();
            if (quantity < 1)
                throw new IllegalArgumentException("quantity must be greater than zero");
            LocalDate validOn = requested.effectiveFrom() == null
                    ? command.startDate()
                    : requested.effectiveFrom();
            PublishedCatalogItem published = commercialSnapshotPort
                    .findPublishedItem(command.priceListId(), command.billingCycle(),
                            requested.catalogItemId(), quantity, validOn)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Published catalog price not found for item: "
                                    + requested.catalogItemId()));
            for (ContractTierLine tierLine : ContractPriceTiers.allocate(quantity,
                    published.tiers())) {
                lines.add(toLine(published, tierLine, requested.effectiveFrom(),
                        requested.effectiveTo()));
            }
        }
        return new ResolvedRequest(SYSTEM_ACTOR, List.copyOf(lines));
    }

    private static SubscriptionItemLineCommand toLine(SubscriptionItemSnapshot snapshot,
            LocalDate effectiveFrom, LocalDate effectiveTo) {
        return new SubscriptionItemLineCommand(snapshot.catalogItemId(), snapshot.itemCode(),
                snapshot.itemName(), snapshot.itemType(), snapshot.capacityUnit(),
                snapshot.tierMin(), snapshot.tierMax(), snapshot.includedQuantity(),
                snapshot.taxTreatment(), snapshot.quantity(), snapshot.unitAmount(),
                snapshot.discountPercent(), snapshot.discountAmount(),
                snapshot.discountIsConditional(), snapshot.taxRate(), effectiveFrom, effectiveTo);
    }

    private static SubscriptionItemLineCommand toLine(PublishedCatalogItem item,
            ContractTierLine tierLine, LocalDate effectiveFrom, LocalDate effectiveTo) {
        return new SubscriptionItemLineCommand(item.catalogItemId(), item.itemCode(),
                item.itemName(), item.itemType(), item.capacityUnit(), tierLine.tier().tierMin(),
                tierLine.tier().tierMax(), tierLine.includedQuantity(),
                tierLine.tier().taxTreatment(), tierLine.quantity(), tierLine.tier().unitAmount(),
                null, null, false, tierLine.tier().taxRate(), effectiveFrom, effectiveTo);
    }

    private record ResolvedRequest(String actor, List<SubscriptionItemLineCommand> items) {
    }
}
