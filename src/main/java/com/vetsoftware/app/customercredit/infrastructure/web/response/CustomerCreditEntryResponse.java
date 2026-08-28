package com.vetsoftware.app.customercredit.infrastructure.web.response;

import com.vetsoftware.app.customercredit.application.dto.CustomerCreditEntryDto;
import com.vetsoftware.app.customercredit.domain.CreditEntryKind;
import com.vetsoftware.app.customercredit.domain.CreditOriginKind;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Un asiento del libro tal como sale por HTTP.
 *
 * <p>
 * Lo ven <strong>los dos</strong> lados: el bloque <em>Cobro y saldos</em>
 * reparte esta tabla como «escribe plataforma, leen ambos», y aqui no hay nada
 * que ocultarle al titular — es su propio dinero y los identificadores de
 * origen son suyos. La restriccion de ese bloque sobre lo que el cliente no
 * puede ver es el codigo de rechazo crudo de la pasarela, que vive en
 * {@code payment_attempts} y no aqui.
 */
public record CustomerCreditEntryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CreditEntryKind entryKind,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal amount, Long lotEntryId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CreditOriginKind originKind,
        Long originPaymentId, Long originDocumentId, Long originSubscriptionId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime occurredAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate valueDate,
        LocalDate expiresOn,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {

    public static CustomerCreditEntryResponse from(CustomerCreditEntryDto dto) {
        return new CustomerCreditEntryResponse(dto.id(), dto.companyId(), dto.entryKind(),
                dto.amount(), dto.lotEntryId(), dto.originKind(), dto.originPaymentId(),
                dto.originDocumentId(), dto.originSubscriptionId(), dto.occurredAt(),
                dto.valueDate(), dto.expiresOn(), dto.createdDate());
    }
}
