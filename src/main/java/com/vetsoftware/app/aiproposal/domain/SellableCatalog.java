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
 *
 * <p>
 * &#9940; <strong>UN CATALOGO SIN NUCLEO COTIZABLE NO ES CONSTRUIBLE.</strong>
 * Es la misma clase de invariante que «un catalogo vacio no puede cotizar una
 * propuesta», que este fichero ya sostenia para la divisa, y esta escrita como
 * invariante y no como un {@code Optional} por lo que costo la version
 * anterior: el nucleo se resolvia con un {@code findFirst()} sobre los
 * articulos con {@code is_core}, el llamante lo filtraba por
 * {@code esCotizable} y el {@code Optional} vacio resultante <em>se ignoraba en
 * silencio</em>. Ni linea de rechazo, ni log, ni contador: 200 con el carrito
 * vacio. Un contrato total no admite esa lectura.
 *
 * @param nucleo
 *            el articulo que todo carrito arrastra, <strong>ya resuelto por el
 *            adaptador</strong> y garantizado cotizable. El dominio no ve
 *            {@code is_core}: ver {@link SellableItem} y la capa anticorrupcion
 *            de {@code JpaSellableCatalogQueryPort}
 */
public record SellableCatalog(Map<String, SellableItem> items, Map<String, List<String>> requires,
        List<PackOffer> packs, SellableItem nucleo) {

    public SellableCatalog {
        if (items == null)
            throw new IllegalArgumentException("catalog items are required");
        if (requires == null)
            throw new IllegalArgumentException("catalog requires edges are required");
        if (packs == null)
            throw new IllegalArgumentException("catalog packs are required");
        items = Map.copyOf(items);
        if (nucleo == null)
            throw new IllegalArgumentException(
                    "a catalog without a quotable core cannot price a proposal");
        if (!nucleo.esCotizable())
            throw new IllegalArgumentException(
                    "the catalog core must be quotable: " + nucleo.code());
        if (!nucleo.equals(items.get(nucleo.code())))
            throw new IllegalArgumentException(
                    "the catalog core must be one of its own items: " + nucleo.code());
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
     * La divisa con la que se cotiza, que es la del nucleo. Un carrito sin divisa
     * no es construible a proposito: es el defecto que dejo 52 de 53 DTO de dinero
     * de este backend mudos sobre la moneda.
     *
     * <p>
     * &#9940; <strong>Total, y sin respaldo por sorteo.</strong> Esto devolvia un
     * {@code Optional} y caia, cuando no habia nucleo, en
     * {@code items.values().stream()...findFirst()} —el orden de iteracion de un
     * {@code Map.copyOf}, que la JVM aleatoriza en cada arranque—. Hoy todo el
     * catalogo es {@code COP} y ese sorteo acertaba por casualidad; con dos divisas
     * conviviendo, el mismo despliegue habria cotizado en una moneda distinta
     * despues de reiniciar. El respaldo desaparecio porque desaparecio su causa:
     * sin nucleo no hay catalogo.
     */
    public String currency() {
        return nucleo.currency();
    }
}
