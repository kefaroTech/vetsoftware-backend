package com.vetsoftware.app.aiproposal.domain;

import java.util.List;
import java.util.Map;

/**
 * La salida del modelo <strong>ya validada</strong>: lo unico que el motor
 * determinista acepta como entrada.
 *
 * <p>
 * Los codigos siguen siendo verbatim —incluidas las alucinaciones—, y eso es
 * deliberado: {@link ProposalCart} es quien los intersecta con el catalogo y
 * quien escribe el veredicto de cada uno, y ese veredicto es la senal con la
 * que se mide si el modelo sirve. Filtrarlos aqui apagaria la senal y dejaria
 * la calidad del modelo sin medir.
 *
 * <p>
 * Lo que si esta resuelto al llegar aqui: los motivos vienen saneados, la
 * coherencia de los dos booleanos esta impuesta por el servidor, y las
 * capacidades estan acotadas.
 */
public record ProposalDraft(boolean understood, boolean outOfDomain, List<String> necessaryCodes,
        List<String> recommendedCodes, Map<String, SanitizedReason> reasons,
        CapacityHint capacities, int contradictoryCodes, List<String> contradictedCodes) {

    public ProposalDraft {
        necessaryCodes = necessaryCodes == null ? List.of() : List.copyOf(necessaryCodes);
        recommendedCodes = recommendedCodes == null ? List.of() : List.copyOf(recommendedCodes);
        reasons = reasons == null ? Map.of() : Map.copyOf(reasons);
        capacities = capacities == null ? CapacityHint.desconocido() : capacities;
        if (contradictoryCodes < 0)
            throw new IllegalArgumentException("contradictoryCodes cannot be negative");
        contradictedCodes = contradictedCodes == null ? List.of() : List.copyOf(contradictedCodes);
        if (!contradictedCodes.isEmpty() && contradictedCodes.size() != contradictoryCodes)
            throw new IllegalArgumentException(
                    "contradictoryCodes must count the contradicted codes it carries");
        if ((outOfDomain || !understood)
                && !(necessaryCodes.isEmpty() && recommendedCodes.isEmpty()))
            throw new IllegalArgumentException(
                    "a draft that understood nothing cannot carry lines");
    }

    /**
     * El borrador vacio: sirve a {@code FUERA_DE_DOMINIO} y a {@code NO_ENTENDIDO}.
     */
    /**
     * La forma de siete argumentos, para quien cuenta la contradiccion pero no
     * puede nombrarla. Delega en la canonica con la lista vacia: la invariante "si
     * hay codigos, la cuenta es su tamano" sigue valiendo, y "hay cuenta sin
     * codigos" queda como estado legitimo y no como incoherencia.
     */
    public ProposalDraft(boolean understood, boolean outOfDomain, List<String> necessaryCodes,
            List<String> recommendedCodes, Map<String, SanitizedReason> reasons,
            CapacityHint capacities, int contradictoryCodes) {
        this(understood, outOfDomain, necessaryCodes, recommendedCodes, reasons, capacities,
                contradictoryCodes, List.of());
    }

    public static ProposalDraft sinLineas(boolean understood, boolean outOfDomain) {
        return sinLineas(understood, outOfDomain, 0);
    }

    /**
     * La forma canonica cuando la contradiccion trae sus codigos: el caso de uso
     * los persiste uno a uno como lineas rechazadas -veredicto
     * {@code NOT_SELLABLE}, plan S8.2.1- y el numero es su tamano. Que la cuenta se
     * derive de la lista es lo que impide que las dos se separen.
     */
    public static ProposalDraft sinLineas(boolean understood, boolean outOfDomain,
            List<String> contradictedCodes) {
        List<String> codigos = contradictedCodes == null
                ? List.of()
                : List.copyOf(contradictedCodes);
        return new ProposalDraft(understood, outOfDomain, List.of(), List.of(), Map.of(),
                CapacityHint.desconocido(), codigos.size(), codigos);
    }

    /**
     * El borrador vacio que ademas <strong>cuenta la contradiccion</strong>: el
     * modelo dijo "fuera de dominio" o "no entendi" y mando codigos de todas
     * formas.
     *
     * <p>
     * Se cuenta en vez de tirarse en silencio porque el plan (S8.2.1) lo pide
     * expresamente: <em>"un modelo que dice «fuera de dominio» y a la vez propone
     * ocho modulos se esta contradiciendo, y eso es una senal de calidad que hay
     * que ver"</em>. Descartar los codigos es lo correcto —no se sirve ni uno—;
     * perder el hecho de que existieron es apagar la senal.
     */
    public static ProposalDraft sinLineas(boolean understood, boolean outOfDomain,
            int contradictoryCodes) {
        return new ProposalDraft(understood, outOfDomain, List.of(), List.of(), Map.of(),
                CapacityHint.desconocido(), contradictoryCodes, List.of());
    }

    /** {@code true} si el modelo se contradijo a si mismo en este turno. */
    public boolean seContradijo() {
        return contradictoryCodes > 0;
    }

    /** Los motivos en la forma que {@link ProposalCart#build} espera. */
    public Map<String, String> textosDeMotivo() {
        return reasons.entrySet().stream().collect(java.util.stream.Collectors
                .toMap(Map.Entry::getKey, entrada -> entrada.getValue().text()));
    }

    public boolean tieneLineas() {
        return !necessaryCodes.isEmpty() || !recommendedCodes.isEmpty();
    }
}
