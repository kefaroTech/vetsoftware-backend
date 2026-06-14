package com.vetsoftware.app.electronicdocument.application.command;

/** Convierte un documento equivalente POS en factura electrónica de venta (a pedido del cliente). */
public record ConvertPosToInvoiceCommand(
        Long posDocumentId,
        Long companyId
) {}
