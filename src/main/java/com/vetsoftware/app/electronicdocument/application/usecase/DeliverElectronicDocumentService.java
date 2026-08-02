package com.vetsoftware.app.electronicdocument.application.usecase;

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
 * Entrega la representación gráfica de un documento VALIDADO: genera QR + PDF, lo guarda en S3 y lo
 * envía por correo al adquiriente (copia al emisor). Idempotente: no reprocesa si el documento ya
 * tiene PDF o no está VALIDADO. Reutilizable por la emisión y por el cierre async de MATIAS
 * (webhook/polling).
 */
@Observed(name = "electronic.document.deliver")
@Component
public class DeliverElectronicDocumentService {
  private static final Logger log = LoggerFactory.getLogger(DeliverElectronicDocumentService.class);

  private final ElectronicDocumentRepository repository;
  private final QrGeneratorPort qrGenerator;
  private final InvoicePdfPort invoicePdf;
  private final InvoiceFileStoragePort fileStorage;
  private final InvoiceMailPort mail;
  private final String qrBaseUrl;

  public DeliverElectronicDocumentService(
      ElectronicDocumentRepository repository,
      QrGeneratorPort qrGenerator,
      InvoicePdfPort invoicePdf,
      InvoiceFileStoragePort fileStorage,
      InvoiceMailPort mail,
      @Value("${vetsoftware.dian.qr-base-url:}") String qrBaseUrl) {
    this.repository = repository;
    this.qrGenerator = qrGenerator;
    this.invoicePdf = invoicePdf;
    this.fileStorage = fileStorage;
    this.mail = mail;
    this.qrBaseUrl = qrBaseUrl;
  }

  public void deliverIfValidated(ElectronicDocument document) {
    if (document.getDianStatus() != DianStatus.VALIDADO) return;
    if (document.getPdfRepresentation() != null && !document.getPdfRepresentation().isBlank())
      return;

    String seal = document.getCufe() != null ? document.getCufe() : document.getCude();
    // La DIAN regula el CONTENIDO del QR (Anexo Técnico): usar el que calculó el proveedor
    // (qrData) cuando exista — es el oficial; si no viene, construir la URL de consulta DIAN
    // (qr-base-url) + CUFE/CUDE como respaldo.
    String qrContent =
        (document.getQrData() != null && !document.getQrData().isBlank())
            ? document.getQrData()
            : qrBaseUrl + (seal == null ? "" : seal);
    String qrBase64 = qrGenerator.generatePngBase64(qrContent);
    byte[] pdf = invoicePdf.render(document, qrBase64);

    String number = numberOf(document);
    String key =
        "invoices/" + document.getCompanyId() + "/" + document.getId() + "/" + number + ".pdf";
    fileStorage.store(key, pdf, "application/pdf");

    document.attachRepresentation(key);
    repository.updateDianResult(document);

    sendEmail(document, number, pdf);
  }

  private void sendEmail(ElectronicDocument document, String number, byte[] pdf) {
    String to = document.getCustomer().email();
    String cc = document.getIssuer().email();
    try {
      mail.send(
          to,
          cc,
          "Factura electrónica " + number,
          "<p>Adjuntamos su factura electrónica <strong>" + number + "</strong>.</p>",
          number + ".pdf",
          pdf);
    } catch (Exception e) {
      // El correo no es bloqueante: el documento ya está validado y su PDF guardado.
      log.warn(
          "No se pudo enviar el correo de la factura {}: {}", document.getId(), e.getMessage());
    }
  }

  private String numberOf(ElectronicDocument document) {
    String prefix = document.getPrefix() == null ? "" : document.getPrefix();
    Object consecutive =
        document.getConsecutive() == null ? document.getId() : document.getConsecutive();
    return (prefix + consecutive).replaceAll("[^A-Za-z0-9_-]", "");
  }
}
