package com.vetsoftware.app.electronicdocument.domain;

/**
 * Se intento emitir una nota credito sobre una factura que ya fue reversada por
 * otra nota credito.
 */
public class DocumentAlreadyReversedException extends RuntimeException {
    public DocumentAlreadyReversedException(Long id) {
        super("El documento " + id + " ya fue reversado por una nota credito.");
    }
}
