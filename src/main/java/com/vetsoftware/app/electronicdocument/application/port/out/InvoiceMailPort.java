package com.vetsoftware.app.electronicdocument.application.port.out;

/** Envía la representación gráfica por correo al adquiriente (con copia al emisor). */
public interface InvoiceMailPort {
    void send(String to, String cc, String subject, String htmlBody, String attachmentName, byte[] attachment);
}
