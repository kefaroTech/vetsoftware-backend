package com.vetsoftware.app.aiproposal.domain;

import java.util.List;
import java.util.Map;

/**
 * La salida del modelo <strong>tal cual llego</strong>, antes de validarla.
 *
 * <p>
 * ⛔ <strong>Todo lo de aqui es entrada no confiable, y no solo la lista de
 * codigos.</strong> El plan afirmaba que "el modelo hace exactamente una cosa y
 * todo lo de aguas abajo es determinista"; la auditoria lo desmintio. El modelo
 * controla ademas:
 *
 * <ul>
 * <li>{@code understood} y {@code outOfDomain}, <strong>dos booleanos que
 * deciden que pantalla ve el prospecto</strong>. Un modelo que devuelva
 * {@code outOfDomain = true} con ocho lineas deja al validador eligiendo entre
 * dos verdades incompatibles;</li>
 * <li>los <strong>tres enteros de capacidad</strong>
 * ({@link CapacityHint});</li>
 * <li>y el <strong>motivo en prosa libre</strong>, que es el unico vector real
 * de S6.4 y el que sanea {@link ProposalReasonSanitizer}.</li>
 * </ul>
 *
 * <p>
 * Este record no valida nada a proposito —es el sobre, no la carta—:
 * {@link ProposalOutputValidator} es quien decide. Solo se defiende de los
 * nulos para que el validador no tenga que hacerlo en cada linea.
 */
public record ModelProposalPayload(boolean understood, boolean outOfDomain,
        List<String> necessaryCodes, List<String> recommendedCodes, Map<String, String> reasons,
        Integer staff, Integer branches, Integer terminals) {

    public ModelProposalPayload {
        necessaryCodes = necessaryCodes == null ? List.of() : List.copyOf(necessaryCodes);
        recommendedCodes = recommendedCodes == null ? List.of() : List.copyOf(recommendedCodes);
        reasons = reasons == null ? Map.of() : Map.copyOf(reasons);
    }

    /** Lo que devuelve un modelo que dijo que no entendio nada. */
    public static ModelProposalPayload noEntendido() {
        return new ModelProposalPayload(false, false, List.of(), List.of(), Map.of(), null, null,
                null);
    }
}
