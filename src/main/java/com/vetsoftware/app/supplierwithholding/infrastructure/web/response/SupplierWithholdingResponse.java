package com.vetsoftware.app.supplierwithholding.infrastructure.web.response;

import com.vetsoftware.app.supplierwithholding.application.dto.SupplierWithholdingDto;
import com.vetsoftware.app.supplierwithholding.domain.SupplierDocumentKind;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholdingType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * La retencion practicada tal como sale por HTTP. Solo la ve la consola de
 * plataforma.
 *
 * <p>
 * <strong>{@code ratePercent} sale con seis decimales y sin redondear.</strong>
 * Es el campo que un front puede estropear solo con formatearlo: mostrar
 * {@code 0.41} en vez de {@code 0.414000} es cosmetica, pero calcular sobre el
 * valor truncado retiene de menos sin dar un error.
 *
 * <p>
 * <strong>No lleva {@code version}</strong> ni {@code municipality_key}: la
 * primera es una barandilla del que escribe y la segunda un centinela del motor
 * que existe para que {@code uq_supplier_withholdings_case} pueda restringir lo
 * que con {@code NULL} no restringia.
 */
public record SupplierWithholdingResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String supplierTaxId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String supplierName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) SupplierDocumentKind supplierDocType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String supplierInvoiceRef,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) SupplierWithholdingType withholdingType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String concept,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal taxableBase,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Porcentaje, no fraccion: el 6,9 por mil es 0.690000.") BigDecimal ratePercent,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal amount,
        @Schema(description = "Codigo DIVIPOLA. Presente solo en las retenciones de ICA.") String municipalityCode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int fiscalYear,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fiscalPeriodKey,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate practicedOn,
        @Schema(description = "Presente solo si ya se emitio el certificado.") LocalDateTime certificateIssuedAt,
        @Schema(description = "Acompaña siempre a certificateIssuedAt.") String certificateRef,
        @Schema(description = "La prueba de la consignacion, cuando llega.") String paymentReceiptRef,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {

    public static SupplierWithholdingResponse from(SupplierWithholdingDto dto) {
        return new SupplierWithholdingResponse(dto.id(), dto.supplierTaxId(), dto.supplierName(),
                dto.supplierDocType(), dto.supplierInvoiceRef(), dto.withholdingType(),
                dto.concept(), dto.taxableBase(), dto.ratePercent(), dto.amount(),
                dto.municipalityCode(), dto.fiscalYear(), dto.fiscalPeriodKey(), dto.practicedOn(),
                dto.certificateIssuedAt(), dto.certificateRef(), dto.paymentReceiptRef(),
                dto.createdDate());
    }
}
