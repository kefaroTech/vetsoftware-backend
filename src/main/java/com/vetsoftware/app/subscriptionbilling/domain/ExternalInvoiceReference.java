package com.vetsoftware.app.subscriptionbilling.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * La referencia de la factura que se emitió <b>fuera</b> de Lumbre.
 *
 * <p>
 * Es todo lo que este software sabe de esa factura: no la calcula, no la
 * numera, no la transmite. Ver {@link IssueStatus} para por qué eso no tiene
 * nada que ver con el motor DIAN del slice {@code electronicdocument}, que sí
 * es parte del producto.
 *
 * <p>
 * Espejo de {@code chk_sbd_external_registered}: número, fecha y proveedor son
 * obligatorios para dar un documento por emitido. <b>El CUFE no</b>, y es una
 * decisión y no un olvido: a veces llega en un segundo paso, y exigirlo aquí
 * bloquearía el registro legítimo de una factura recién emitida. Los
 * registrados sin CUFE son la consulta (b) de la conciliación R17.
 */
public record ExternalInvoiceReference(String invoiceNumber, String cufe, LocalDate issuedAt,
        String provider, LocalDateTime registeredAt, Long registeredBySystemUserId) {

    public ExternalInvoiceReference {
        if (invoiceNumber == null || invoiceNumber.isBlank())
            throw new IllegalArgumentException("external invoice number is required");
        if (invoiceNumber.length() > 60)
            throw new IllegalArgumentException("external invoice number must be 60 chars or less");
        if (cufe != null && cufe.length() > 100)
            throw new IllegalArgumentException("external cufe must be 100 chars or less");
        if (issuedAt == null)
            throw new IllegalArgumentException("external issued date is required");
        if (provider == null || provider.isBlank())
            throw new IllegalArgumentException("external provider is required");
        if (provider.length() > 40)
            throw new IllegalArgumentException("external provider must be 40 chars or less");
    }

    /**
     * La fecha desde la que se cuenta el vencimiento, que es <b>la fiscal</b>.
     *
     * <p>
     * Contarlo desde que se calculó el cobro aquí suspendería cuentas por un
     * retraso administrativo propio, no del cliente: si el documento se calculó el
     * 1 y la factura se emitió fuera el 20, un plazo de 15 días medido desde el
     * cálculo deja la cuenta en mora cinco días antes de que el cliente tuviera
     * nada que pagar.
     */
    public LocalDate fechaDeReferenciaParaElVencimiento() {
        return issuedAt;
    }
}
