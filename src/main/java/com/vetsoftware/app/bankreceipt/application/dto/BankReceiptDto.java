package com.vetsoftware.app.bankreceipt.application.dto;

import com.vetsoftware.app.bankreceipt.domain.BankReceipt;
import com.vetsoftware.app.bankreceipt.domain.BankReceiptStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * La entrada del extracto tal como la consume la aplicacion.
 *
 * <p>
 * <strong>Sin {@code version}</strong>: el numero de version es la barandilla
 * del bloqueo optimista, no un dato del expediente. Publicarlo invitaria a un
 * cliente a mandarlo de vuelta y a construir un control de concurrencia
 * paralelo al que ya hace Hibernate.
 */
public record BankReceiptDto(Long id, String bankAccountRef, String bankReference,
        LocalDate receivedOn, BigDecimal amount, String description, BankReceiptStatus status,
        LocalDateTime identifiedAt, LocalDateTime createdDate) {

    public static BankReceiptDto from(BankReceipt receipt) {
        return new BankReceiptDto(receipt.getId(), receipt.getBankAccountRef(),
                receipt.getBankReference(), receipt.getReceivedOn(), receipt.getAmount(),
                receipt.getDescription(), receipt.getStatus(), receipt.getIdentifiedAt(),
                receipt.getCreatedDate());
    }
}
