package com.vetsoftware.app.electronicdocument.infrastructure.representation;

import com.vetsoftware.app.electronicdocument.application.port.out.InvoiceMailPort;
import com.vetsoftware.app.infrastructure.email.EmailDispatchOutcome;
import com.vetsoftware.app.infrastructure.email.ResendEmailClient;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;

/**
 * Envía la representación gráfica (PDF) de la factura electrónica por Resend,
 * con copia al emisor. El gate y el remitente los controla
 * {@link ResendEmailClient}.
 *
 * <p>
 * Traduce el desenlace del cliente de correo al vocabulario del puerto. La
 * traducción existe para que {@code application} no importe nada de
 * {@code infrastructure}: son dos enumeraciones con los mismos tres valores a
 * propósito, y este {@code switch} es la costura entre ambas capas.
 */
@Component
public class MailInvoiceSender implements InvoiceMailPort {

    private final ResendEmailClient email;

    public MailInvoiceSender(ResendEmailClient email) {
        this.email = email;
    }

    @Override
    public CompletableFuture<DeliveryOutcome> send(String to, String cc, String subject,
            String htmlBody, String attachmentName, byte[] attachment) {
        List<ResendEmailClient.Attachment> attachments = attachment == null
                ? List.of()
                : List.of(new ResendEmailClient.Attachment(attachmentName, attachment));
        return email.send(to, cc, subject, htmlBody, attachments).thenApply(MailInvoiceSender::map);
    }

    private static DeliveryOutcome map(EmailDispatchOutcome outcome) {
        return switch (outcome) {
            case ACCEPTED -> DeliveryOutcome.ACCEPTED;
            case SKIPPED -> DeliveryOutcome.SKIPPED;
            case FAILED -> DeliveryOutcome.FAILED;
        };
    }
}
