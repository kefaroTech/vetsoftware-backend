package com.vetsoftware.app.aiproposal.infrastructure.web.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * La propuesta entera, y lo unico que sale por los cuatro endpoints.
 *
 * @param discardedLines
 *            cuantas lineas no se pudieron cotizar, <strong>sin codigos, sin
 *            veredictos y sin desglose</strong>. Va porque la pantalla necesita
 *            poder decir "no todo lo que propusimos se puede contratar", no
 *            para depurar, y no distingue causas a proposito
 * @param recommendations
 *            el bloque "tambien podria interesarte": llega sin marcar y
 *            <strong>no suma al total</strong>
 * @param recalculated
 *            {@code false} cuando se agotaron los tres ajustes y la propuesta
 *            vuelve intacta. Es un campo y no un 400 porque el usuario no hizo
 *            nada mal
 */
public record AssistantProposalResponse(String token, String presentation, LocalDateTime expiresAt,
        Long version, List<AssistantProposalLineResponse> lines,
        List<AssistantProposalLineResponse> recommendations, int discardedLines, String currency,
        BigDecimal subtotal, BigDecimal taxes, BigDecimal total, BigDecimal firstPeriodTotal,
        AssistantPackOfferResponse packOffer, int refinementsLeft, boolean recalculated) {
}
