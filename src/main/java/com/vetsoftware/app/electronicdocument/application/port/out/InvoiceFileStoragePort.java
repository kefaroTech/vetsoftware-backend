package com.vetsoftware.app.electronicdocument.application.port.out;

/** Guarda el archivo (PDF/XML) de la factura y devuelve su clave/referencia. */
public interface InvoiceFileStoragePort {

    /** Sube el contenido bajo la clave indicada y devuelve la clave almacenada. */
    String store(String key, byte[] content, String contentType);

    /**
     * Elimina el objeto por su clave.
     *
     * <p>
     * Existe para compensar una subida cuya escritura en base de datos fallo
     * despues: sin este metodo, el PDF ya subido se queda en el bucket sin ninguna
     * fila que lo referencie y sin nadie que lo borre. Tiene que ser idempotente —
     * borrar una clave que ya no esta no es un error.
     */
    void delete(String key);
}
