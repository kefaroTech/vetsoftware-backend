package com.vetsoftware.app.electronicdocument.application.port.out;

import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import java.time.LocalDateTime;

/**
 * Resultado normalizado de transmitir un documento a un proveedor. El `status` puede ser TERMINAL
 * (VALIDADO/RECHAZADO) o PENDIENTE (proveedores async como MATIAS, que completan luego por webhook o por
 * polling de estado). Campos de sellos vacíos hasta que el proveedor los devuelva.
 */
public record ProviderResult(
        DianStatus status,
        String prefix,
        Long consecutive,
        String cufe,
        String cude,
        String uuid,
        String xmlSigned,
        String qrData,
        String qrUrl,
        String pdfRepresentation,
        String providerDocumentKey,
        String rejectionReason,
        LocalDateTime validationDate,
        Integer httpStatus,
        String rawResponse
) {}
