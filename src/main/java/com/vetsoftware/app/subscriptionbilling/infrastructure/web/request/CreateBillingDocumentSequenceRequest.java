package com.vetsoftware.app.subscriptionbilling.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Declarar una serie del consecutivo interno: {@code DC}, {@code NC},
 * {@code ND}.
 *
 * <p>
 * El patrón acota a mayúsculas sin separadores porque el prefijo se concatena
 * al número impreso ({@code DC-000001}) y un guion dentro del prefijo lo
 * volvería ambiguo de leer.
 */
public record CreateBillingDocumentSequenceRequest(
        @NotBlank @Size(max = 10) @Pattern(regexp = "[A-Z]{1,10}") String prefix) {
}
