package com.vetsoftware.app.electronicdocument.infrastructure.representation;

import com.vetsoftware.app.electronicdocument.application.port.out.InvoiceMailPort;
import com.vetsoftware.app.infrastructure.email.ResendEmailClient;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Envía la representación gráfica (PDF) de la factura electrónica por Resend, con copia al emisor.
 * El envío es best-effort (el caller ya captura fallos); el gate y el remitente los controla
 * {@link ResendEmailClient}.
 */
@Component
public class MailInvoiceSender implements InvoiceMailPort {

    private final ResendEmailClient email;

    public MailInvoiceSender(ResendEmailClient email) {
        this.email = email;
    }

    @Override
    public void send(String to, String cc, String subject, String htmlBody,
                     String attachmentName, byte[] attachment) {
        List<ResendEmailClient.Attachment> attachments = attachment == null
                ? List.of()
                : List.of(new ResendEmailClient.Attachment(attachmentName, attachment));
        email.send(to, cc, subject, htmlBody, attachments);
    }
}
