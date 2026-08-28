package com.vetsoftware.app.smmlvvalue.infrastructure.web.request;

import com.vetsoftware.app.smmlvvalue.domain.SmmlvStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * {@code statusReference} y {@code statusChangedOn} van sin {@code @NotNull} a
 * proposito: son obligatorios solo cuando el estado no es {@code IN_FORCE}, y
 * esa es una regla condicional que el dominio comprueba —igual que
 * {@code chk_smmlv_values_status}—, no una restriccion de campo.
 */
public record ChangeSmmlvStatusRequest(@NotNull SmmlvStatus status,
        @Size(max = 255) String statusReference, LocalDate statusChangedOn) {
}
