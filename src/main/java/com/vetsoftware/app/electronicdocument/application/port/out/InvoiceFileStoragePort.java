package com.vetsoftware.app.electronicdocument.application.port.out;

/** Guarda el archivo (PDF/XML) de la factura y devuelve su clave/referencia. */
public interface InvoiceFileStoragePort {
  String store(String key, byte[] content, String contentType);
}
