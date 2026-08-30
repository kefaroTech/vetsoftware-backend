package com.vetsoftware.app.aiproposal.infrastructure.web.response;

import java.math.BigDecimal;

/**
 * Una linea de la propuesta, tal como la pinta la pantalla 2.
 *
 * <p>
 * &#9940; <strong>Sin {@code verdict} y sin {@code source}, y solo salen las
 * aceptadas.</strong> Los cinco veredictos se persisten -son la senal con la
 * que se mide si el modelo sirve- pero este endpoint es <em>driveable</em>: el
 * texto que produce los codigos lo escribe quien pregunta, asi que
 * serializarlos convertiria la respuesta en un oraculo de cinco valores sobre
 * el catalogo interno ("&#191;existe?", "&#191;esta en borrador?", "&#191;esta
 * retirado?", "&#191;se vende por autoservicio?").
 *
 * <p>
 * <strong>Con {@code currency}</strong>, que 52 de 53 DTO de dinero de este
 * backend no llevan.
 */
public record AssistantProposalLineResponse(String code, String name, String description,
        String kind, int quantity, BigDecimal unitAmount, BigDecimal taxRate, BigDecimal taxAmount,
        BigDecimal totalAmount, int trialDays, String currency, String reason) {
}
