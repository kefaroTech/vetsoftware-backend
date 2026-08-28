package com.vetsoftware.app.gatewaysettlement.application.command;

/**
 * Llego la factura del proveedor de la pasarela: sus dos datos se escriben a la
 * vez.
 *
 * <p>
 * <strong>Los dos campos son obligatorios aunque las columnas sean
 * nulables.</strong> Lo nulable es el estado «todavia no ha llegado la
 * factura», no «la factura llego a medias»:
 * {@code chk_gateway_settlements_provider_invoice} exige que esten los dos o
 * ninguno, y sin el NIT no se puede armar el reporte anual de terceros.
 */
public record AttachProviderInvoiceCommand(Long id, String providerInvoiceRef,
        String providerTaxId) {
}
