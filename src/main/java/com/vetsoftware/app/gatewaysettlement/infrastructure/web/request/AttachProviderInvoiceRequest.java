package com.vetsoftware.app.gatewaysettlement.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * <strong>Los dos campos son obligatorios aunque las columnas sean
 * nulables.</strong> Lo nulable en el esquema es el estado «todavia no ha
 * llegado la factura», no «la factura llego a medias»:
 * {@code chk_gateway_settlements_provider_invoice} exige que esten los dos o
 * ninguno. Sin la referencia no hay soporte que enseñar si rechazan la
 * deduccion; sin el NIT no se puede armar el reporte anual de terceros.
 */
public record AttachProviderInvoiceRequest(
        @NotBlank(message = "Debes indicar la referencia de la factura del proveedor.") @Size(max = 60, message = "La referencia de la factura no puede superar los 60 caracteres.") String providerInvoiceRef,
        @NotBlank(message = "Debes indicar el NIT del proveedor.") @Size(max = 50, message = "El NIT del proveedor no puede superar los 50 caracteres.") String providerTaxId) {
}
