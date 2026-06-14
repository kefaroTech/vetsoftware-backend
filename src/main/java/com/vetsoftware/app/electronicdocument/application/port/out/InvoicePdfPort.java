package com.vetsoftware.app.electronicdocument.application.port.out;

import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;

/** Renderiza la representación gráfica (PDF) del documento, con el QR embebido. */
public interface InvoicePdfPort {
    byte[] render(ElectronicDocument document, String qrPngBase64);
}
