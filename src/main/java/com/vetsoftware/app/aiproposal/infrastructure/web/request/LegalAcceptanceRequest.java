package com.vetsoftware.app.aiproposal.infrastructure.web.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Una casilla marcada: el par {@code (code, documentVersion)} tal cual lo
 * devuelve {@code LegalConsentCheckbox} del front publico.
 */
public record LegalAcceptanceRequest(@NotBlank @Size(max = 50) String code,
        @Min(1) int documentVersion) {
}
