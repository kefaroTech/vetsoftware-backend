package com.vetsoftware.app.quote.infrastructure.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Quien acepta. La IP y la fecha NO viajan aqui: las pone el servidor desde la
 * peticion y el reloj, porque una prueba que el cliente puede escribir no
 * prueba nada.
 */
public record AcceptQuoteRequest(@NotBlank @Email @Size(max = 120) String acceptedByEmail) {
}
