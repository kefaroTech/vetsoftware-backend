package com.vetsoftware.app.subscriptionbilling.infrastructure.web.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Los cargos que agrupa la nota crédito.
 *
 * <p>
 * Tienen que ser todos del mismo signo —negativo—; lo comprueba el dominio, que
 * es donde se pueden leer sus importes. Una nota crédito con signos mezclados
 * hace que la conciliación deje de cuadrar sin dar ninguna señal.
 */
public record IssueCreditNoteRequest(@NotEmpty List<Long> chargeIds) {
}
