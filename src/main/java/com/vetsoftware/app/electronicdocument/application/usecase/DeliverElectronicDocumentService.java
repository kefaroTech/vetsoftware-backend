package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.port.out.DocumentDeliveryMetrics;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.InvoiceFileStoragePort;
import com.vetsoftware.app.electronicdocument.application.port.out.InvoiceMailPort;
import com.vetsoftware.app.electronicdocument.application.port.out.InvoicePdfPort;
import com.vetsoftware.app.electronicdocument.application.port.out.QrGeneratorPort;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Entrega la representación gráfica de un documento VALIDADO: genera QR + PDF,
 * lo guarda en S3 y lo envía por correo al adquiriente (copia al emisor).
 * Idempotente: no reprocesa si el documento ya tiene PDF o no está VALIDADO.
 * Reutilizable por la emisión y por el cierre async de MATIAS
 * (webhook/polling).
 *
 * <p>
 * <b>Esta clase NO lleva {@code @Transactional}, y es deliberado.</b> Sube a S3
 * y manda un correo, y la regla dura {@code SIN_IO_EXTERNO_EN_TRANSACCION} de
 * {@code HexagonalArchitectureTest} prohíbe hacer I/O externo dentro de una
 * transacción — sus cinco llamadores ya lo dejan escrito. No lo "arregles"
 * envolviéndolo en una: rompería el build y el pool de conexiones.
 *
 * <p>
 * La otra mitad de ese patrón es la compensación, que sí vive aquí: si la
 * escritura de la referencia falla después de que el PDF ya subió, se borra el
 * objeto del bucket y se relanza el fallo original. Ver
 * {@link #compensarSubida} para lo que ese <i>best effort</i> no alcanza a
 * cubrir.
 */
@Observed(name = "electronic.document.deliver")
@Component
public class DeliverElectronicDocumentService {
    private static final Logger log = LoggerFactory
            .getLogger(DeliverElectronicDocumentService.class);

    private final ElectronicDocumentRepository repository;
    private final QrGeneratorPort qrGenerator;
    private final InvoicePdfPort invoicePdf;
    private final InvoiceFileStoragePort fileStorage;
    private final InvoiceMailPort mail;
    private final DocumentDeliveryMetrics deliveryMetrics;
    private final String qrBaseUrl;

    public DeliverElectronicDocumentService(ElectronicDocumentRepository repository,
            QrGeneratorPort qrGenerator, InvoicePdfPort invoicePdf,
            InvoiceFileStoragePort fileStorage, InvoiceMailPort mail,
            DocumentDeliveryMetrics deliveryMetrics,
            @Value("${vetsoftware.dian.qr-base-url:}") String qrBaseUrl) {
        this.repository = repository;
        this.qrGenerator = qrGenerator;
        this.invoicePdf = invoicePdf;
        this.fileStorage = fileStorage;
        this.mail = mail;
        this.deliveryMetrics = deliveryMetrics;
        this.qrBaseUrl = qrBaseUrl;
    }

    public void deliverIfValidated(ElectronicDocument document) {
        if (document.getDianStatus() != DianStatus.VALIDADO)
            return;
        if (document.getPdfRepresentation() != null && !document.getPdfRepresentation().isBlank())
            return;

        String seal = document.getCufe() != null ? document.getCufe() : document.getCude();
        // La DIAN regula el CONTENIDO del QR (Anexo Técnico): usar el que calculó el
        // proveedor
        // (qrData) cuando exista — es el oficial; si no viene, construir la URL de
        // consulta DIAN
        // (qr-base-url) + CUFE/CUDE como respaldo.
        String qrContent = (document.getQrData() != null && !document.getQrData().isBlank())
                ? document.getQrData()
                : qrBaseUrl + (seal == null ? "" : seal);
        String qrBase64 = qrGenerator.generatePngBase64(qrContent);
        byte[] pdf = invoicePdf.render(document, qrBase64);

        String number = numberOf(document);
        String key = "invoices/" + document.getCompanyId() + "/" + document.getId() + "/" + number
                + ".pdf";
        fileStorage.store(key, pdf, "application/pdf");

        // El PDF YA está en S3. Si la escritura de la referencia falla, el objeto se
        // queda en el bucket sin ninguna fila que lo apunte. Y updateDianResult puede
        // fallar de verdad, no en teoría: relee el documento (y lanza
        // ElectronicDocumentNotFoundException si desapareció) y después guarda sobre
        // una
        // entidad con @Version, así que un OptimisticLockException contra el job de
        // contingencia de la DIAN es un caso real. Lo que queda tirado no es un byte
        // cualquiera: es la representación gráfica de una factura fiscal fuera de todo
        // inventario, invisible para la retención, el borrado y la reexpedición, porque
        // todos esos procesos parten de la fila. Por eso se compensa aquí mismo.
        try {
            document.attachRepresentation(key);
            repository.updateDianResult(document);
        } catch (RuntimeException e) {
            compensarSubida(key, e);
            throw e;
        }

        sendEmail(document, number, pdf);
    }

    private void sendEmail(ElectronicDocument document, String number, byte[] pdf) {
        String to = document.getCustomer().email();
        String cc = document.getIssuer().email();
        try {
            mail.send(to, cc, "Factura electrónica " + number,
                    "<p>Adjuntamos su factura electrónica <strong>" + number + "</strong>.</p>",
                    number + ".pdf", pdf);
            deliveryMetrics.delivered();
        } catch (Exception e) {
            // ERROR y no WARN: no existe camino de recuperación. No hay reintento ni
            // outbox, y el guard de idempotencia de deliverIfValidated dará el documento
            // por entregado en la siguiente pasada porque el PDF ya quedó adjunto. El
            // correo se pierde de forma definitiva aunque la petición del usuario
            // responda 200 — la severidad codifica la accionabilidad, no el código de
            // estado. Cuando exista la cola de reintento (issue del outbox) esto pasará a
            // ser WARN, porque entonces sí habrá recuperación.
            //
            // La excepción va como último argumento y NO como e.getMessage(): así
            // conserva tipo, causa y stacktrace, pasa por la redacción de
            // RedactedThrowable en RedactingAppender y llega entera a Loki. Con
            // getMessage() una NullPointerException escribía literalmente ": null".
            log.error("No se pudo enviar el correo de la factura {}", document.getId(), e);
            // El contador es lo que hace la pérdida contable y alertable: el log
            // diagnostica un caso, la métrica responde «¿a cuántos clientes no les llegó
            // su factura la semana pasada?».
            deliveryMetrics.deliveryFailed();
        }
    }

    /**
     * Borra el PDF recién subido tras un fallo al grabar su referencia. Nunca
     * lanza: el fallo del borrado es secundario y no puede tapar la excepción
     * original, que es la que explica por qué falló la entrega. Si el borrado
     * falla, se adjunta como {@code suppressed} de la original y se registra la
     * clave del objeto, que es el único rastro con el que después se puede
     * encontrar el huérfano en el bucket.
     *
     * <p>
     * Es <i>best effort</i>, y conviene no venderlo como otra cosa: solo cubre el
     * fallo de la escritura. Si el proceso muere entre el {@code store} y este
     * {@code catch} —un kill del contenedor, un OOM, una parada del despliegue— el
     * huérfano queda igual y nadie lo borra. Cerrar ese hueco de verdad exigiría
     * una tabla de pendientes y un job de barrido, que están fuera de alcance aquí.
     */
    private void compensarSubida(String key, RuntimeException falloOriginal) {
        try {
            fileStorage.delete(key);
            // WARN y no INFO: no es el curso normal, y su frecuencia es el dato que decide
            // si algún día hace falta la tabla de pendientes + job de barrido.
            log.warn("Se borró el PDF de factura {} tras fallar la escritura de su referencia"
                    + " en base de datos", key);
        } catch (RuntimeException falloDelBorrado) {
            falloOriginal.addSuppressed(falloDelBorrado);
            log.error("PDF de factura huérfano en el bucket: falló el borrado compensatorio"
                    + " de {}. Es la representación gráfica de un documento fiscal que ninguna"
                    + " fila referencia, así que ningún proceso lo alcanza; hay que eliminarlo"
                    + " a mano.", key, falloDelBorrado);
        }
    }

    private String numberOf(ElectronicDocument document) {
        String prefix = document.getPrefix() == null ? "" : document.getPrefix();
        Object consecutive = document.getConsecutive() == null
                ? document.getId()
                : document.getConsecutive();
        return (prefix + consecutive).replaceAll("[^A-Za-z0-9_-]", "");
    }
}
