package com.vetsoftware.app.aiproposal.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * La foto del catalogo con la que el motor determinista toma todas sus
 * decisiones.
 *
 * <p>
 * <strong>Solo arcos {@code REQUIRES}.</strong> Los {@code RECOMMENDS} no
 * entran en esta estructura, y no es un olvido: auto-anadir un recomendado es
 * un upsell disfrazado de requisito tecnico (plan S4.4, paso 5). Al no existir
 * el arco, el cierre no puede seguirlo ni por accidente el dia que alguien
 * toque el bucle. Los {@code EXCLUDES} son cero en el catalogo real.
 *
 * <p>
 * Es un valor inmutable: las tres colecciones se copian en el constructor
 * compacto, asi que quien la construya no puede mutarla despues por debajo del
 * motor.
 */
public record SellableCatalog(Map<String, SellableItem> items, Map<String, List<String>> requires,
        List<PackOffer> packs) {

    public SellableCatalog {
        if (items == null)
            throw new IllegalArgumentException("catalog items are required");
        if (requires == null)
            throw new IllegalArgumentException("catalog requires edges are required");
        if (packs == null)
            throw new IllegalArgumentException("catalog packs are required");
        items = Map.copyOf(items);
        Map<String, List<String>> copiaDeArcos = new LinkedHashMap<>();
        requires.forEach((desde, hacia) -> copiaDeArcos.put(desde, List.copyOf(hacia)));
        requires = Collections.unmodifiableMap(copiaDeArcos);
        packs = List.copyOf(packs);
    }

    public Optional<SellableItem> find(String code) {
        return code == null ? Optional.empty() : Optional.ofNullable(items.get(code));
    }

    /** Los codigos que {@code code} arrastra. Nunca {@code null}. */
    public List<String> requiredBy(String code) {
        return requires.getOrDefault(code, List.of());
    }

    /**
     * El articulo marcado {@code is_core}, que entra siempre (plan S4.4, paso 3).
     */
    /**
     * La huella de la foto del catalogo, para
     * {@code ai_proposals.catalog_snapshot_hash}: 64 caracteres hexadecimales en
     * minusculas, que es lo que exige {@code AiProposal}.
     *
     * <p>
     * &#9888; <strong>No es la misma huella que la del prompt.</strong>
     * {@code ProposalPromptBuilder} digiere ademas el bloque de hints, y esa huella
     * no existe todavia cuando esta se escribe: la cabecera se persiste y se
     * commitea <em>antes</em> de armar el prompt, porque la invocacion tiene que
     * caer fuera de toda transaccion. Lo que se congela aqui es exactamente lo que
     * decide el precio -codigo, importe, impuesto, prueba y si se puede vender-,
     * que es lo que hace falta para saber si dos propuestas se cotizaron contra el
     * mismo catalogo.
     *
     * <p>
     * Ordenado por codigo a proposito: {@code items} es un mapa y su orden de
     * iteracion no es contrato, asi que sin ordenar la misma foto daria huellas
     * distintas en dos JVM.
     */
    public String snapshotHash() {
        StringBuilder plano = new StringBuilder();
        items.keySet().stream().sorted().forEach(code -> {
            SellableItem item = items.get(code);
            plano.append(code).append('|').append(item.unitAmount().toPlainString()).append('|')
                    .append(item.taxRate().toPlainString()).append('|').append(item.trialDays())
                    .append('|').append(item.active()).append('|')
                    .append(item.selfServiceEligible()).append('|').append(item.currency())
                    .append('\n');
        });
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(plano.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException imposible) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", imposible);
        }
    }

    /**
     * La divisa con la que se cotiza. Un carrito sin divisa no es construible a
     * proposito: es el defecto que dejo 52 de 53 DTO de dinero de este backend
     * mudos sobre la moneda.
     */
    public Optional<String> currency() {
        return core().map(SellableItem::currency)
                .or(() -> items.values().stream().map(SellableItem::currency).findFirst());
    }

    public Optional<SellableItem> core() {
        return items.values().stream().filter(SellableItem::core).findFirst();
    }
}
