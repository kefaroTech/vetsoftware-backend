package com.vetsoftware.app.electronicdocument.infrastructure.web.request;

import com.vetsoftware.app.electronicdocument.domain.CreditNoteReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * {@code partialAmount} opcional: null ⇒ nota crédito TOTAL (anulación); un
 * monto (≤ total) ⇒ nota PARCIAL.
 *
 * <p>
 * <strong>Por qué lleva nombre propio en el contrato.</strong> Hay otro
 * {@code IssueCreditNoteRequest} en
 * {@code subscriptionbilling.infrastructure.web.request}, y springdoc funde los
 * esquemas <em>por nombre simple de clase</em>, no por paquete. Los dos records
 * tienen campos <strong>disjuntos</strong> —aquel declara {@code chargeIds};
 * este, {@code reason} y {@code partialAmount}—, así que la fusión no degrada
 * un campo: publica <em>el cuerpo del otro endpoint</em>. Ganaba el de
 * facturación de suscripciones por orden de escaneo, y
 * {@code POST /electronic-documents/{id}/credit-note} quedaba anunciando
 * {@code chargeIds} cuando lo que el servidor exige es {@code reason}.
 *
 * <p>
 * No era una mentira teórica: el front del tenant ya llama a ese endpoint
 * mandando {@code reason}, y por eso <b>no podía</b> atarlo con un
 * {@code MatchesContract} —el contrato lo declaraba con la otra forma—. Justo
 * en el único sitio donde el nombre estaba mal, la barandilla no existía.
 *
 * <p>
 * <strong>Por qué el prefijo va en este lado y no en el otro.</strong> El
 * nombre simple lo publica hoy el record de {@code subscriptionbilling} y la
 * consola de plataforma ya lo tiene atado en su {@code api.contract.ts}:
 * moverlo allí cambiaría el esquema bajo un consumidor vivo, mientras que
 * moverlo aquí hace nacer uno nuevo y ningún consumidor actual se entera.
 * Además {@code Electronic} es el discriminante real —esta es la nota crédito
 * <em>electrónica</em> que va a la DIAN y responde
 * {@code ElectronicDocumentDto}; aquella agrupa cargos de suscripción y
 * responde {@code BillingDocumentResponse}—. El precedente de la técnica son
 * {@code LimitDimensionSubModuleSummary} y
 * {@code RegisterOutageAffectedCompanyRequest}.
 *
 * <p>
 * El nombre de la clase Java <strong>no</strong> cambia: lo que colisiona es el
 * nombre publicado, no el símbolo.
 */
@Schema(name = "IssueElectronicCreditNoteRequest")
public record IssueCreditNoteRequest(
        @NotNull(message = "Debes seleccionar el motivo de la nota crédito.") CreditNoteReason reason,
        @Positive(message = "El monto parcial debe ser mayor que cero.") BigDecimal partialAmount) {
}
