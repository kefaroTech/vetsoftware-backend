package com.vetsoftware.app.electronicdocument.application.port.out;

/**
 * Genera el código QR de la factura (PNG en base64, listo para embeber en el
 * HTML como data URI).
 */
public interface QrGeneratorPort {
    String generatePngBase64(String content);
}
