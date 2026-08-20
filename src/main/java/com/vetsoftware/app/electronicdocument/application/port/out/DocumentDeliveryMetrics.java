package com.vetsoftware.app.electronicdocument.application.port.out;

/**
 * Telemetría de la entrega por correo de la representación gráfica de un
 * documento electrónico.
 *
 * <p>
 * <b>Por qué un método por resultado y no un parámetro enum:</b> así el
 * conjunto de valores de la etiqueta {@code result} queda cerrado por el propio
 * contrato. Ningún llamador puede introducir un valor nuevo que la lista blanca
 * de cardinalidad descartaría <em>en silencio</em> — el fallo se convertiría en
 * un error de compilación en vez de en una serie que nunca aparece en
 * Prometheus.
 *
 * <p>
 * <b>Por qué también se cuenta el éxito:</b> sin denominador, una alerta de
 * tasa no puede distinguir «fallaron 5 de 5» de «fallaron 5 de 5.000». Un SLI
 * es eventos buenos sobre eventos válidos (SRE Workbook), no un contador de
 * errores suelto.
 *
 * <p>
 * <b>Lo que este contrato NO hace:</b> no recupera el correo perdido. Hace
 * <em>visible</em> la pérdida; evitarla exige una cola o un outbox de
 * reintento, que no existe hoy en ninguno de los cuatro puntos del sistema que
 * envían correo.
 */
public interface DocumentDeliveryMetrics {

    /** El proveedor de correo aceptó el envío. */
    void delivered();

    /**
     * El envío falló y, al no haber reintento, el correo se perdió de forma
     * definitiva.
     */
    void deliveryFailed();
}
