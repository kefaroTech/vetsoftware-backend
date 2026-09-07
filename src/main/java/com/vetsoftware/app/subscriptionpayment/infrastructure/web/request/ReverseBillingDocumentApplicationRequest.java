package com.vetsoftware.app.subscriptionpayment.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Por que se revierte la aplicacion. Sin el, el reverso solo dice "se revirtio
 * $X" y un auditor tiene que reconstruir el motivo cruzando el resto del
 * expediente a mano.
 */
public record ReverseBillingDocumentApplicationRequest(@NotBlank @Size(max = 255) String reason) {
}
