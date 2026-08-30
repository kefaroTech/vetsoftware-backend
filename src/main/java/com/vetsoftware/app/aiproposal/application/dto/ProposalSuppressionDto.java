package com.vetsoftware.app.aiproposal.application.dto;

import com.vetsoftware.app.aiproposal.application.port.out.ProposalRetentionPort;
import java.time.LocalDateTime;

/**
 * Lo que movio una supresion, desglosado por tabla.
 *
 * <p>
 * &#9940; <strong>El desglose no es cosmetico.</strong> Un unico total deja
 * indistinguibles dos respuestas que significan cosas opuestas: "ese correo no
 * esta en el sistema" y "el paso que borra los motivos no toco nada porque su
 * subconsulta esta rota". La segunda es un incumplimiento silencioso; la
 * primera, una noche normal.
 *
 * <p>
 * <strong>No devuelve el correo.</strong> Quien pregunta ya lo escribio, y
 * devolverlo lo mete en el cuerpo de una respuesta que puede acabar en un log
 * de acceso o en el historial de una consola.
 *
 * @param suppressedAt
 *            cuando se atendio esta peticion. Es el mismo instante que quedo
 *            escrito en {@code ai_proposal_suppression_requests}: quien atiende
 *            un habeas data tiene que poder responderle al titular con una
 *            fecha, y hasta ahora el contrato no publicaba ninguna, asi que el
 *            front se inventaba una con el reloj del navegador
 * @param previouslySuppressedAt
 *            cuando se atendio la peticion anterior del mismo titular, o
 *            {@code null} si es la primera. Sin este dato, "cero filas" no
 *            distingue "ya se le habia borrado" de "nunca hubo nada suyo": el
 *            primer borrado se lleva el hash por el que se busca, asi que la
 *            segunda vez sale igual que un correo que no estuvo nunca
 */
public record ProposalSuppressionDto(int proposals, int turns, int lines, int total,
        LocalDateTime suppressedAt, LocalDateTime previouslySuppressedAt) {

    public static ProposalSuppressionDto from(ProposalRetentionPort.SuppressionResult resultado,
            LocalDateTime suppressedAt) {
        return new ProposalSuppressionDto(resultado.proposals(), resultado.turns(),
                resultado.lines(), resultado.total(), suppressedAt,
                resultado.previouslySuppressedAt());
    }
}
