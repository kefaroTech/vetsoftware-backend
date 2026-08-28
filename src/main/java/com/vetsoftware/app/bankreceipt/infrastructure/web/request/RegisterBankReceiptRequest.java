package com.vetsoftware.app.bankreceipt.infrastructure.web.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * <strong>Sin {@code companyId}, y aqui no hay ninguno que pudiera ir.</strong>
 * La regla dura {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} mira todo
 * {@code @RequestBody} sin mirar la ruta ni el rol, pero en este caso ni
 * siquiera hace falta invocarla: la tabla no tiene empresa porque antes de
 * identificar la entrada no se sabe de quien es.
 *
 * @param amount
 *            <strong>sin {@code @Positive}, y es deliberado.</strong> El
 *            {@code CHECK} del esquema es {@code amount <> 0}: un cargo del
 *            banco o la devolucion de un cheque entran en el extracto con signo
 *            negativo. Poner aqui el {@code @Positive} que llevan las demas
 *            tablas de dinero rechazaria en el binder la mitad de un extracto
 *            real, y con un mensaje de campo invalido que el operario no sabria
 *            como corregir. El unico importe prohibido es el cero, y esa regla
 *            vive en el constructor de la entidad, que es donde el CLAUDE.md
 *            pide las invariantes
 */
public record RegisterBankReceiptRequest(
        @NotBlank(message = "Debes indicar la cuenta bancaria del extracto.") @Size(max = 60, message = "La cuenta bancaria no puede superar los 60 caracteres.") String bankAccountRef,
        @NotBlank(message = "Debes indicar la referencia del banco.") @Size(max = 120, message = "La referencia del banco no puede superar los 120 caracteres.") String bankReference,
        @NotNull(message = "Debes indicar la fecha en que se recibio el dinero.") LocalDate receivedOn,
        @NotNull(message = "El valor de la consignacion es obligatorio.") @Digits(integer = 17, fraction = 2, message = "El valor no puede tener mas de dos decimales.") BigDecimal amount,
        @Size(max = 255, message = "La descripcion no puede superar los 255 caracteres.") String description) {
}
