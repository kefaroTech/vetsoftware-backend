package com.vetsoftware.app.aiproposal.application.dto;

import com.vetsoftware.app.aiproposal.application.port.out.ProposalRetentionPort;

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
 */
public record ProposalSuppressionDto(int proposals, int turns, int lines, int total) {

    public static ProposalSuppressionDto from(ProposalRetentionPort.SuppressionResult resultado) {
        return new ProposalSuppressionDto(resultado.proposals(), resultado.turns(),
                resultado.lines(), resultado.total());
    }
}
