package com.vetsoftware.app.electronicdocument.infrastructure.web.request;

import com.vetsoftware.app.electronicdocument.application.command.SaleLineKind;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.electronicdocument.domain.PaymentMeans;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;

/**
 * Venta de POS a registrar. El front nunca manda companyId (lo deriva el backend del JWT). El unitPrice de
 * cada linea es el precio final (post-promo) con IVA incluido; la clasificacion tributaria la pone el
 * servidor desde el catalogo. customerOwnerId null o finalConsumer=true => consumidor final anonimo.
 */
public record RegisterPosSaleRequest(
        @NotNull ElectronicDocumentType documentType,
        boolean finalConsumer,
        Long customerOwnerId,
        @NotEmpty @Valid List<SaleLineRequest> lines,
        @NotEmpty @Valid List<SalePaymentRequest> payments,
        /** Idempotencia de la venta: UUID que el front genera por apertura del cobro (opcional). */
        String clientRequestId
) {
    public record SaleLineRequest(
            @NotNull SaleLineKind kind,
            Long refId,
            String description,
            @NotNull @Positive BigDecimal quantity,
            @NotNull @PositiveOrZero BigDecimal unitPrice
    ) {}

    public record SalePaymentRequest(
            @NotNull PaymentMeans means,
            @NotNull @PositiveOrZero BigDecimal amount
    ) {}
}
