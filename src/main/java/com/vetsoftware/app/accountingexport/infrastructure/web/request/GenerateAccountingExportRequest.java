package com.vetsoftware.app.accountingexport.infrastructure.web.request;

import com.vetsoftware.app.accountingexport.domain.AccountingExportKind;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * <strong>Sin {@code companyId} por ninguna via</strong>: la exportacion es de
 * los libros de VetSoftware.
 *
 * <p>
 * <strong>Sin {@code attemptNumber} ni
 * {@code generatedBySystemUserId}</strong>, y las dos ausencias son
 * deliberadas: el intento lo calcula el caso de uso y la firma sale de
 * {@code authz.currentSystemUserId()}. Aceptar la segunda por el cuerpo dejaria
 * firmar un fichero a nombre de otro superadministrador.
 *
 * @param totalDebit
 *            tiene que cuadrar con {@code totalCredit}. Que cuadren lo
 *            comprueba el dominio —es una regla entre dos campos— y lo respalda
 *            {@code chk_accounting_exports_balanced}
 */
public record GenerateAccountingExportRequest(
        @NotBlank(message = "Debes indicar el periodo contable.") @Pattern(regexp = "^[0-9]{4}-(0[1-9]|1[0-2])$", message = "El periodo debe tener la forma aaaa-MM.") String periodKey,
        @NotNull(message = "Debes indicar la clase de exportacion.") AccountingExportKind exportKind,
        @NotNull(message = "Debes indicar el total debito.") @PositiveOrZero(message = "El total debito no puede ser negativo.") @Digits(integer = 17, fraction = 2, message = "El total debito admite como maximo 2 decimales.") BigDecimal totalDebit,
        @NotNull(message = "Debes indicar el total credito.") @PositiveOrZero(message = "El total credito no puede ser negativo.") @Digits(integer = 17, fraction = 2, message = "El total credito admite como maximo 2 decimales.") BigDecimal totalCredit,
        @NotBlank(message = "Debes indicar la huella del fichero.") @Pattern(regexp = "^[0-9a-f]{64}$", message = "La huella debe ser un SHA-256 de 64 caracteres hexadecimales en minusculas.") @Schema(description = "SHA-256 del contenido del fichero, en minusculas.") String totalsHash,
        @NotBlank(message = "Debes indicar donde esta el fichero.") @Size(max = 255, message = "La referencia del fichero no puede superar los 255 caracteres.") String fileRef) {
}
