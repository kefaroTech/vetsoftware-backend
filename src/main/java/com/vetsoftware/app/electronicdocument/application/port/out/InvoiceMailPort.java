package com.vetsoftware.app.electronicdocument.application.port.out;

import java.util.concurrent.CompletableFuture;

/**
 * Envía la representación gráfica por correo al adquiriente (con copia al
 * emisor).
 *
 * <p>
 * <b>Devuelve un futuro y no {@code void} a propósito (issue #242).</b> El
 * envío real ocurre en otro hilo y el adaptador no lanza nunca, así que un
 * {@code void} obligaba al caso de uso a inventarse el desenlace: contaba como
 * entregado todo lo que lograra encolar, y su rama de fallo era inalcanzable
 * para el 100 % de los fallos reales del proveedor. Con el futuro, el resultado
 * llega a quien lo mide.
 */
public interface InvoiceMailPort {

    /**
     * @return futuro con el desenlace del envío. <b>Nunca se completa de forma
     *         excepcional</b>: un fallo del proveedor llega como
     *         {@link DeliveryOutcome#FAILED}, no como excepción. Lo que sí puede
     *         lanzar es la propia llamada, de forma síncrona, si el pool de envío
     *         rechaza la tarea.
     */
    CompletableFuture<DeliveryOutcome> send(String to, String cc, String subject, String htmlBody,
            String attachmentName, byte[] attachment);

    /**
     * Desenlace del envío desde la perspectiva del documento.
     *
     * <p>
     * Tres valores y no un booleano porque «no se envió» y «falló el envío» son
     * hechos distintos para la métrica: el primero es el modo normal de un entorno
     * sin correo y no debe contaminar la tasa de error.
     */
    enum DeliveryOutcome {
        /** El proveedor aceptó el mensaje. */
        ACCEPTED,
        /** El correo está deshabilitado en este entorno; no se intentó enviar. */
        SKIPPED,
        /** El mensaje no salió y no hay reintento: el cliente no recibió su factura. */
        FAILED
    }
}
